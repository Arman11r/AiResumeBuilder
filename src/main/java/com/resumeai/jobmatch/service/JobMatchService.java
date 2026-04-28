package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.*;
import java.util.List;

public interface JobMatchService {
    JobMatchResponse analyzeJobFit(AnalyzeJobFitRequest request);
    List<JobMatchResponse> getMatchesByUser(String userId);
    List<JobMatchResponse> getMatchesByResume(String resumeId);
    List<JobMatchResponse> getTopMatches(String userId, int limit);
    List<JobMatchResponse> getBookmarkedMatches(String userId);
    JobMatchResponse bookmarkMatch(String matchId);
    List<JobMatchResponse> fetchJobsFromLinkedIn(FetchJobsRequest request);
    List<JobMatchResponse> fetchJobsFromNaukri(FetchJobsRequest request);
    void deleteMatch(String matchId);
}