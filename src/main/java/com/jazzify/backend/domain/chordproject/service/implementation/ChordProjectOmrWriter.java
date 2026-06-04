package com.jazzify.backend.domain.chordproject.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.repository.ChordProjectRepository;
import com.jazzify.backend.domain.chordproject.util.IRealProChordParser;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.session.repository.SessionRepository;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class ChordProjectOmrWriter {

	private static final int FAILED_MESSAGE_MAX_LENGTH = 500;

	private final ChordProjectRepository chordProjectRepository;
	private final SessionRepository sessionRepository;
	private final ChordInfoWriter chordInfoWriter;

	public ChordProject createPending(
		User user,
		String title,
		MusicKey key,
		String timeSignature,
		@Nullable String requestedTitle,
		@Nullable MusicKey requestedKey,
		@Nullable String requestedTimeSignature
	) {
		Session session = sessionRepository.save(Session.builder()
			.title(title)
			.build());
		ChordProject project = ChordProject.builder()
			.title(title)
			.keySignature(key)
			.timeSignature(timeSignature)
			.user(user)
			.session(session)
			.build();
		project.markOmrQueued(requestedTitle, requestedKey, requestedTimeSignature);
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

	public void complete(UUID projectPublicId, String title, MusicKey key, String timeSignature, String progression) {
		chordProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> {
				project.updateOmrResolvedFields(title, key, timeSignature);
				chordInfoWriter.deleteAllByChordProject(project);
				chordInfoWriter.saveAll(IRealProChordParser.parse(progression, timeSignature, project));
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

	private static String truncate(@Nullable String failureReason) {
		if (failureReason == null || failureReason.isBlank()) {
			return "OMR 처리 중 알 수 없는 오류가 발생했습니다.";
		}
		return failureReason.length() <= FAILED_MESSAGE_MAX_LENGTH
			? failureReason
			: failureReason.substring(0, FAILED_MESSAGE_MAX_LENGTH);
	}
}
