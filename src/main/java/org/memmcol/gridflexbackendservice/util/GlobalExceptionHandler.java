package org.memmcol.gridflexbackendservice.util;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

	Map<String, String> errorMessage = new HashMap<>();

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGenericException(Exception ex, WebRequest request) {
		ex.printStackTrace();
		String msg = "An unexpected error occurred: " + "";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(SQLServerException.class)
	public ResponseEntity<?> handleSQLServerException(SQLServerException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(SQLException.class)
	public ResponseEntity<?> handleSQLException(SQLException ex) {
		String msg = "Internal server Error";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<?> handleNullPointerException(NullPointerException ex) {
		String msg = "Internal server Error"; // We encountered a problem while processing your request
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(IndexOutOfBoundsException.class)
	public ResponseEntity<?> handleIndexOutOfBoundsException(IndexOutOfBoundsException ex) {
		String msg = "Internal server Error";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
		String msg = "Invalid argument";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(ArithmeticException.class)
	public ResponseEntity<?> handleArithmeticException(ArithmeticException ex) {
		String msg = "We encountered a problem while performing a calculation";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(FileNotFoundException.class)
	public ResponseEntity<?> handleFileNotFoundException(FileNotFoundException ex) {
		String msg = "The file or resource you're looking for could not be found";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<?> handleIOException(IOException ex) {
		String msg = "There was an issue with processing your file";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex) {
		String msg = "Resources not found";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(NumberFormatException.class)
	public ResponseEntity<?> handleNumberFormatException(NumberFormatException ex) {
		String msg = "The data provided isn't in the correct format";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(ArrayIndexOutOfBoundsException.class)
	public ResponseEntity<?> handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException ex) {
		String msg = "We're encountering difficulties accessing the requested data";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(ClassCastException.class)
	public ResponseEntity<?> handleClassCastException(ClassCastException ex) {
		String msg = "It seems we encountered an unexpected error while processing your request";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<?> handleDataAccessException(DataAccessException ex) {
		String msg = "There's a problem with accessing some data";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex) {
		String msg = "Resources not found";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex) {
		String msg = "Uauthorized";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		Map<String, String> msg = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			msg.put(fieldName, errorMessage);
		});
		// String msg = "Bad request";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg.toString());
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
		String msg = "Authentication failed";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
	}

	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<?> handleAuthorizationException(AuthorizationException ex) {
		String msg = "Authorization forbidden";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMessage);
	}

	@ExceptionHandler(MethodNotAllowedException.class)
	public ResponseEntity<?> handleMethodNotAllowedException(MethodNotAllowedException ex) {
		String msg = "The action you're trying to perform is not supported.";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorMessage);
	}

	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<?> handleConcurrencyFailureException(ConcurrencyFailureException ex) {
		String msg = "We encountered a problem while processing your request. Concurrency failure.";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(DataNotFoundException.class)
	public ResponseEntity<?> DataNotFoundException(DataNotFoundException ex) {
		String msg = "Data not found";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(UncategorizedSQLException.class)
	public ResponseEntity<Object> handleUncategorizedSQLException(UncategorizedSQLException ex, WebRequest request) {
		String msg = "The offset specified in a OFFSET clause may not be negative";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);

	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<?> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
		String parameterName = ex.getParameterName();
		String msg = String.format("Required request parameter '%s' is not present", parameterName);
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		String msg = String.format("Request method '%s' is not supported for this endpoint. Supported methods are %s.",
				ex.getMethod(), ex.getSupportedHttpMethods());
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(CannotCreateTransactionException.class)
	public ResponseEntity<?> handleCannotCreateTransactionException(CannotCreateTransactionException ex,
			WebRequest request) {
		ex.printStackTrace();
		String msg = "Unable to connect to the database. Please try again later";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<?> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
			WebRequest request) {
		// Log the exception for debugging purposes
		ex.printStackTrace();

		// Create a meaningful response
		String msg = "Data integrity violation";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		String msg = "Malformed JSON request";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(RestClientException.class)
	public ResponseEntity<Object> handleRestClientException(RestClientException ex) {
		Map<String, Object> body = new HashMap<>();
//		body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
//		body.put("message", "Failed to communicate with external service: " + "");
		String msg = "Failed to send OTP email";
		errorMessage.put("responsecode", HttpStatus.INTERNAL_SERVER_ERROR.toString());
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorMessage);
	}

	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public class ResourceNotFoundException extends RuntimeException {
		public ResourceNotFoundException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public class AuthorizationException extends RuntimeException {
		public AuthorizationException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public class DataNotFoundException extends RuntimeException {
		public DataNotFoundException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public class UnauthorizedException extends RuntimeException {
		public UnauthorizedException(String message) {
			super(message);
		}
	}

	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public class SQLServerException extends RuntimeException {
		public SQLServerException(String message) {
			super(message);
		}
	}
}
