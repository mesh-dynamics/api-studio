package com.cube.dao;

import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.cube.agent.UtilException;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingStatus;

public class RecordingBuilder {

	private String customerId;
	private String app;
	private String instanceId;
	private String collection;
	private Optional<String> id;
	private RecordingStatus status;
	private Optional<Instant> timestamp;
	private String templateVersion;
	private Optional<String> parentRecordingId;
	private String rootRecordingId;
	private String name;
	private String label;
	private Optional<String> codeVersion;
	private Optional<String> branch;
	private List<String> tags;
	private boolean archived;
	private Optional<String> gitCommitId;
	private Optional<String> collectionUpdOpSetId;
	private Optional<String> templateUpdOpSetId;
	private Optional<String> comment;
	private String userId;
	private Optional<String> generatedClassJarPath;
	private Optional<URLClassLoader> generatedClassLoader;

	public RecordingBuilder(CubeMetaInfo cubeMetaInfo, String collection) {
		this.customerId = cubeMetaInfo.customerId;
		this.app = cubeMetaInfo.app;
		this.instanceId = cubeMetaInfo.instance;
		this.collection = collection;
		this.status = RecordingStatus.Running;
		this.timestamp = Optional.of(Instant.now());
		this.templateVersion = DEFAULT_TEMPLATE_VER;
		this.id = Optional.empty();
		//recalculateId();
		this.parentRecordingId = Optional.empty();
		this.rootRecordingId = "";
		this.name = "";
		this.codeVersion = Optional.empty();
		this.branch = Optional.empty();
		this.tags = Collections.emptyList();
		this.archived = false;
		this.gitCommitId = Optional.empty();
		this.collectionUpdOpSetId = Optional.empty();
		this.templateUpdOpSetId = Optional.empty();
		this.comment = Optional.empty();
		this.userId = "";
		this.generatedClassJarPath = Optional.empty();
		this.generatedClassLoader = Optional.empty();
		this.label = "";
	}

	/**
	 * @return
	 */
	public Recording build() {
	    String idv = id.orElse(this.recalculateId());
		if (this.rootRecordingId.isEmpty()) {
			rootRecordingId = idv;
		}
		return new Recording(idv, customerId, app, instanceId, collection, status, timestamp
			, templateVersion, parentRecordingId, rootRecordingId, name, codeVersion, branch
		, tags, archived, gitCommitId, collectionUpdOpSetId, templateUpdOpSetId, comment
			, userId, generatedClassJarPath, generatedClassLoader, label);
	}

	private String recalculateId() {
		return ReqRespStoreSolr.Types.Recording.toString().concat("-")
			.concat(String.valueOf(Math.abs(Objects.hash(customerId, app,
				collection, templateVersion))));
	}

	private void populateClassLoader() throws Exception {
		generatedClassJarPath.ifPresent(UtilException.rethrowConsumer(jarPath -> {
			Path path = Paths.get(jarPath);
			this.generatedClassLoader = Optional.of(new URLClassLoader(
				new URL[]{path.toUri().toURL()},
				this.getClass().getClassLoader()
			));
		}));
	}

	public RecordingBuilder withId(String id) {
	    this.id = Optional.of(id);
	    return this;
    }

	public RecordingBuilder withStatus(RecordingStatus status) {
		this.status = status;
		return this;
	}

	public RecordingBuilder withUpdateTimestamp(Instant timestamp) {
		this.timestamp = Optional.ofNullable(timestamp);
		return this;
	}

	public RecordingBuilder withTemplateSetVersion(String version) {
		this.templateVersion = version;
		//recalculateId();
		return this;
	}

	public RecordingBuilder withParentRecordingId(String parentRecordingId) {
		this.parentRecordingId = Optional.ofNullable(parentRecordingId);
		return this;
	}

	public RecordingBuilder withRootRecordingId(String rootRecordingId) {
		this.rootRecordingId = rootRecordingId;
		return this;
	}

	public RecordingBuilder withName(String name) {
		this.name = name;
		return this;
	}

    public RecordingBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

	public RecordingBuilder withCodeVersion(String codeVersion) {
		this.codeVersion = Optional.ofNullable(codeVersion);
		return this;
	}

	public RecordingBuilder withBranch(String branchName) {
		this.branch = Optional.ofNullable(branchName);
		return this;
	}

	public RecordingBuilder withTags(List<String> tags) {
		this.tags = tags;
		return this;
	}

	public RecordingBuilder withArchived(boolean archived) {
		this.archived = archived;
		return this;
	}

	public RecordingBuilder withGitCommitId(String commitId) {
		this.gitCommitId = Optional.ofNullable(commitId);
		return this;
	}

	public RecordingBuilder withCollectionUpdateOpSetId(String collectionUpdateOpSetId) {
		this.collectionUpdOpSetId = Optional.ofNullable(collectionUpdateOpSetId);
		return this;
	}

	public RecordingBuilder withTemplateUpdateOpSetId(String templateUpdateOpSetId) {
		this.templateUpdOpSetId = Optional.ofNullable(templateUpdateOpSetId);
		return this;
	}

	public RecordingBuilder withComment(String comment) {
		this.comment = Optional.ofNullable(comment);
		return this;
	}

	public RecordingBuilder withUserId(String userId) {
		this.userId = userId;
		return this;
	}

	public RecordingBuilder withGeneratedClassJarPath(String generatedClassJarPath) throws  Exception {
		this.generatedClassJarPath = Optional.ofNullable(generatedClassJarPath);
		populateClassLoader();
		return this;
	}

}
