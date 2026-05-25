package com.jazzify.backend.domain.sheetproject.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordproject.util.IRealProChordParser;
import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.repository.SheetProjectRepository;
import com.jazzify.backend.domain.storagefile.dto.response.StorageFileResponse;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.service.StorageFileService;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileReader;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class SheetProjectOmrWriter {

	private static final int FAILED_MESSAGE_MAX_LENGTH = 500;

	private final SheetProjectRepository sheetProjectRepository;
	private final SheetProjectWriter sheetProjectWriter;
	private final SheetFileWriter sheetFileWriter;
	private final ChordInfoWriter chordInfoWriter;
	private final StorageFileService storageFileService;
	private final StorageFileReader storageFileReader;

	public SheetProject createPending(
		User user,
		byte[] fileData,
		String originalFileName,
		String contentType,
		String title,
		@Nullable MusicKey key
	) {
		StorageFileResponse storageFileResponse = storageFileService.upload(originalFileName, contentType, fileData);
		StorageFile storageFile = storageFileReader.getByPublicId(storageFileResponse.publicId());

		FileType fileType = FileType.fromFileName(storageFile.getOriginalFileName());
		SheetFile sheetFile = sheetFileWriter.create(fileType);
		storageFile.linkToSheetFile(sheetFile);

		SheetProject project = sheetProjectWriter.create(title, key, user, sheetFile);
		project.markOmrQueued();
		return project;
	}

	public void markProcessing(UUID projectPublicId, int progress) {
		sheetProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> project.markOmrProcessing(normalizeProgress(progress)));
	}

	/**
	 * OMR 서버 제출 후 job_id 를 저장하고 처리 중 상태로 전환한다.
	 *
	 * @param projectPublicId 프로젝트 식별자
	 * @param omrJobId        OMR 서버에서 발급된 job ID
	 * @param progress        진행도 (0~100)
	 */
	public void storeJobIdAndMarkProcessing(UUID projectPublicId, String omrJobId, int progress) {
		sheetProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> {
				project.storeOmrJobId(omrJobId);
				project.markOmrProcessing(normalizeProgress(progress));
			});
	}

	public void complete(UUID projectPublicId, String title, @Nullable MusicKey key, String timeSignature, String progression) {
		sheetProjectRepository.findByPublicId(projectPublicId)
			.ifPresent(project -> {
				project.updateOmrResolvedFields(title, key);
				chordInfoWriter.deleteAllBySheetProject(project);
				chordInfoWriter.saveAll(IRealProChordParser.parseForSheetProject(progression, timeSignature, project));
				project.markOmrCompleted();
			});
	}

	public void fail(UUID projectPublicId, @Nullable String failureReason, int progress) {
		sheetProjectRepository.findByPublicId(projectPublicId)
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

