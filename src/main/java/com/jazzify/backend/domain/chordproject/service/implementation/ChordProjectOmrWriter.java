package com.jazzify.backend.domain.chordproject.service.implementation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrChord;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrSourceType;
import com.jazzify.backend.domain.chordproject.repository.ChordProjectRepository;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class ChordProjectOmrWriter {

	private static final int FAILED_MESSAGE_MAX_LENGTH = 500;

	private final ChordProjectRepository chordProjectRepository;
	private final ChordInfoWriter chordInfoWriter;
	private final EntityManager entityManager;

	public ChordProject createPending(
		User user,
		String title,
		MusicKey key,
		String timeSignature,
		@Nullable String requestedTitle,
		@Nullable MusicKey requestedKey,
		@Nullable String requestedTimeSignature,
		ChordProjectOmrSourceType sourceType
	) {
		Session session = Session.builder()
			.title(title)
			.build();
		entityManager.persist(session);
		ChordProject project = ChordProject.builder()
			.title(title)
			.keySignature(key)
			.timeSignature(timeSignature)
			.user(user)
			.session(session)
			.build();
		project.markOmrQueued(requestedTitle, requestedKey, requestedTimeSignature, sourceType);
		return chordProjectRepository.save(project);
	}

	public void markProcessing(UUID projectPublicId, int progress) {
		chordProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> project.markOmrProcessing(normalizeProgress(progress)));
	}

	public void storeJobIdAndMarkProcessing(UUID projectPublicId, String omrJobId, int progress) {
		chordProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> {
				project.storeOmrJobId(omrJobId);
				project.markOmrProcessing(normalizeProgress(progress));
			});
	}

	public void complete(
		UUID projectPublicId,
		String title,
		MusicKey key,
		String timeSignature,
		int sourceBeatsPerBar,
		List<ChordProjectOmrChord> chords
	) {
		chordProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> {
				project.updateOmrResolvedFields(title, key, timeSignature);
				chordInfoWriter.deleteAllByChordProject(project);
				chordInfoWriter.saveAll(toChordInfos(chords, sourceBeatsPerBar, timeSignature, project));
				project.markOmrCompleted();
			});
	}

	public void fail(UUID projectPublicId, @Nullable String failureReason, int progress) {
		chordProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> project.markOmrFailed(truncate(failureReason), normalizeProgress(progress)));
	}

	private static int normalizeProgress(int progress) {
		return Math.max(0, Math.min(progress, 100));
	}

	private static List<ChordInfo> toChordInfos(
		List<ChordProjectOmrChord> chords,
		int sourceBeatsPerBar,
		String timeSignature,
		ChordProject project
	) {
		int targetBeatsPerBar = extractBeatsPerBar(timeSignature);
		int normalizedSourceBeats = sourceBeatsPerBar > 0 ? sourceBeatsPerBar : targetBeatsPerBar;
		double beatScale = (double) targetBeatsPerBar / normalizedSourceBeats;

		List<ChordProjectOmrChord> ordered = new ArrayList<>(chords);
		ordered.sort(Comparator
			.comparingInt(ChordProjectOmrChord::bar)
			.thenComparingDouble(ChordProjectOmrChord::beat));
		ordered = mergeConsecutive(ordered);

		List<ChordInfo> result = new ArrayList<>();
		for (ChordProjectOmrChord chord : ordered) {
			double beat = scaleBeat(chord.beat(), beatScale, targetBeatsPerBar);
			double duration = scaleDuration(chord.durationBeats(), beatScale, beat, targetBeatsPerBar);
			result.add(ChordInfo.builder()
				.chord(chord.chord())
				.bar(chord.bar())
				.beat(beat)
				.durationBeats(duration)
				.sortOrder(result.size() + 1)
				.chordProject(project)
				.session(project.getSession())
				.build());
		}
		return result;
	}

	private static List<ChordProjectOmrChord> mergeConsecutive(List<ChordProjectOmrChord> chords) {
		if (chords.isEmpty()) {
			return chords;
		}

		List<ChordProjectOmrChord> merged = new ArrayList<>();
		ChordProjectOmrChord current = chords.getFirst();
		for (int i = 1; i < chords.size(); i++) {
			ChordProjectOmrChord next = chords.get(i);
			if (current.bar() == next.bar()
				&& Objects.equals(current.chord(), next.chord())
				&& Double.compare(current.beat() + current.durationBeats(), next.beat()) == 0) {
				current = new ChordProjectOmrChord(
					current.bar(),
					current.chord(),
					current.beat(),
					current.durationBeats() + next.durationBeats()
				);
			} else {
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);
		return merged;
	}

	private static int extractBeatsPerBar(String timeSignature) {
		try {
			int beatsPerBar = Integer.parseInt(timeSignature.split("/")[0].trim());
			if (beatsPerBar <= 0) {
				throw ChordProjectErrorCode.INVALID_TIME_SIGNATURE.toException();
			}
			return beatsPerBar;
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			throw ChordProjectErrorCode.INVALID_TIME_SIGNATURE.toException();
		}
	}

	private static double scaleBeat(double sourceBeat, double beatScale, int targetBeatsPerBar) {
		double normalizedBeat = Double.isFinite(sourceBeat) ? sourceBeat : 1.0;
		double scaledBeat = 1.0 + ((Math.max(1.0, normalizedBeat) - 1.0) * beatScale);
		return Math.min(scaledBeat, targetBeatsPerBar);
	}

	private static double scaleDuration(
		double sourceDuration,
		double beatScale,
		double beat,
		int targetBeatsPerBar
	) {
		double normalizedDuration = Double.isFinite(sourceDuration) ? Math.max(0.0, sourceDuration) : 0.0;
		double scaledDuration = normalizedDuration * beatScale;
		double remainingDuration = targetBeatsPerBar + 1.0 - beat;
		return Math.min(scaledDuration, remainingDuration);
	}

	private static String truncate(@Nullable String failureReason) {
		if (failureReason == null || failureReason.isBlank()) {
			return "OMR 처리 중 알 수 없는 오류가 발생했습니다.";
		}
		return failureReason.length() <= FAILED_MESSAGE_MAX_LENGTH
			? failureReason
			: failureReason.substring(0, FAILED_MESSAGE_MAX_LENGTH);
	}
}
