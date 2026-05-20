//package com.example.todo.common.exception;
//
//import org.springframework.data.core.PropertyReferenceException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.method.annotation.HandlerMethodValidationException;
//
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiError> handleValidationException(
//            MethodArgumentNotValidException ex
//    ) {
//        Map<String, String> errors = new LinkedHashMap<>();
//
//        ex.getBindingResult()
//                .getFieldErrors()
//                .forEach(error -> errors.put(
//                        error.getField(),
//                        error.getDefaultMessage()
//                ));
//
//        ApiError apiError = new ApiError(
//                HttpStatus.BAD_REQUEST.value(),
//                "Validation failed",
//                errors
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(apiError);
//    }
//
//    @ExceptionHandler(HandlerMethodValidationException.class)
//    public ResponseEntity<ApiError> handleRequestParamValidationException(
//            HandlerMethodValidationException ex
//    ) {
//        Map<String, String> errors = new LinkedHashMap<>();
//
//        ex.getParameterValidationResults()
//                .forEach(result -> {
//                    String parameterName = result.getMethodParameter().getParameterName();
//
//                    result.getResolvableErrors()
//                            .forEach(error -> errors.put(
//                                    parameterName != null ? parameterName : "parameter",
//                                    error.getDefaultMessage()
//                            ));
//                });
//
//        ApiError apiError = new ApiError(
//                HttpStatus.BAD_REQUEST.value(),
//                "Request parameter validation failed",
//                errors
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(apiError);
//    }
//
//    @ExceptionHandler({
//            TaskNotFoundException.class,
//            UserNotFoundException.class,
//            CommentNotFoundException.class
//    })
//    public ResponseEntity<ApiError> handleNotFoundException(RuntimeException ex) {
//        ApiError apiError = new ApiError(
//                HttpStatus.NOT_FOUND.value(),
//                ex.getMessage()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.NOT_FOUND)
//                .body(apiError);
//    }
//
//    @ExceptionHandler(DuplicateResourceException.class)
//    public ResponseEntity<ApiError> handleDuplicateResourceException(
//            DuplicateResourceException ex
//    ) {
//        ApiError apiError = new ApiError(
//                HttpStatus.CONFLICT.value(),
//                ex.getMessage()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.CONFLICT)
//                .body(apiError);
//    }
//
//    @ExceptionHandler(BadRequestException.class)
//    public ResponseEntity<ApiError> handleBadRequestException(
//            BadRequestException ex
//    ) {
//        ApiError apiError = new ApiError(
//                HttpStatus.BAD_REQUEST.value(),
//                ex.getMessage()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(apiError);
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ApiError> handleIllegalArgumentException(
//            IllegalArgumentException ex
//    ) {
//        ApiError apiError = new ApiError(
//                HttpStatus.BAD_REQUEST.value(),
//                ex.getMessage()
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(apiError);
//    }
//
//    @ExceptionHandler(PropertyReferenceException.class)
//    public ResponseEntity<ApiError> handlePropertyReferenceException(
//            PropertyReferenceException ex
//    ) {
//        ApiError apiError = new ApiError(
//                HttpStatus.BAD_REQUEST.value(),
//                "Invalid sort field"
//        );
//
//        return ResponseEntity
//                .status(HttpStatus.BAD_REQUEST)
//                .body(apiError);
//    }
//}


package com.example.todo.common.exception;

import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleRequestParamValidationException(
            HandlerMethodValidationException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getParameterValidationResults()
                .forEach(result -> {
                    String parameterName = result.getMethodParameter().getParameterName();

                    result.getResolvableErrors()
                            .forEach(error -> errors.put(
                                    parameterName != null ? parameterName : "parameter",
                                    error.getDefaultMessage()
                            ));
                });

        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Request parameter validation failed",
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    @ExceptionHandler({
            TaskNotFoundException.class,
            UserNotFoundException.class,
            CommentNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFoundException(RuntimeException ex) {
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(apiError);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResourceException(
            DuplicateResourceException ex
    ) {
        ApiError apiError = new ApiError(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequestException(
            BadRequestException ex
    ) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handlePropertyReferenceException(
            PropertyReferenceException ex
    ) {
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid sort field"
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }
}