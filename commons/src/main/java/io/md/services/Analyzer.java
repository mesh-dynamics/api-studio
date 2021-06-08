package io.md.services;

import java.util.Optional;

import io.md.dao.Analysis;

/*
 * Created by IntelliJ IDEA.
 * Date: 21/07/20
 */
public interface Analyzer {
	/**
	 * @param replayId
	 * @return
	 */
	public Optional<Analysis> analyze(String replayId, Optional<String> templateVersion);
}
