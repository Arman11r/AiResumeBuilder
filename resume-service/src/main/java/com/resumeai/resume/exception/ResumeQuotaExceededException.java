package com.resumeai.resume.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ResumeQuotaExceededException extends RuntimeException {
    public ResumeQuotaExceededException() {
        super("Resume quota exceeded. Free plan allows a maximum of 3 resumes.");
    }
}