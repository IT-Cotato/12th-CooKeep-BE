package com.cookeep.cookeep.common.exception;

public class EntityNotFoundException extends AppException {

	public EntityNotFoundException(ErrorCode errorCode) {
		super(errorCode);
	}
}