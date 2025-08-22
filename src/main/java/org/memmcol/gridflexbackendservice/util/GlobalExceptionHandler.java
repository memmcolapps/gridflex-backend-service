package org.memmcol.gridflexbackendservice.util;

import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

import javax.security.sasl.AuthenticationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

	Map<String, String> errorMessage = new HashMap<>();

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGenericException(Exception ex, WebRequest request) {
		ex.printStackTrace();
		String msg = "An unexpected error occurred: " + ex.getMessage();
		errorMessage.put("responsecode", "100");
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
		ex.printStackTrace();
		String msg = "Internal server Error [See server logs for more details]";
		errorMessage.put("responsecode", "101");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<?> handleNullPointerException(NullPointerException ex) {
		ex.printStackTrace();
		String msg = "Internal server Error [See server logs for more details]";; // We encountered a problem while processing your request
		errorMessage.put("responsecode", "102");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(IndexOutOfBoundsException.class)
	public ResponseEntity<?> handleIndexOutOfBoundsException(IndexOutOfBoundsException ex) {
		ex.printStackTrace();
		String msg = "Internal server Error [See server logs for more details]";;
		errorMessage.put("responsecode", "103");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
		ex.printStackTrace();
		String msg = "Invalid argument [See server logs for more details]";
		errorMessage.put("responsecode", "104");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(ArithmeticException.class)
	public ResponseEntity<?> handleArithmeticException(ArithmeticException ex) {
		ex.printStackTrace();
		String msg = "We encountered a problem while performing a calculation";
		errorMessage.put("responsecode", "105");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(FileNotFoundException.class)
	public ResponseEntity<?> handleFileNotFoundException(FileNotFoundException ex) {
		ex.printStackTrace();
		String msg = "The file or resource you're looking for could not be found";
		errorMessage.put("responsecode", "106");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<?> handleIOException(IOException ex) {
		ex.printStackTrace();
		String msg = "There was an issue with processing your file";
		errorMessage.put("responsecode", "107");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex) {
		ex.printStackTrace();
		String msg = "Resources not found [See server logs for more details]";
		errorMessage.put("responsecode", "108");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(NumberFormatException.class)
	public ResponseEntity<?> handleNumberFormatException(NumberFormatException ex) {
		ex.printStackTrace();
		String msg = "The data provided isn't in the correct format";
		errorMessage.put("responsecode", "109");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(ArrayIndexOutOfBoundsException.class)
	public ResponseEntity<?> handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException ex) {
		ex.printStackTrace();
		String msg = "We're encountering difficulties accessing the requested data";
		errorMessage.put("responsecode", "110");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(ClassCastException.class)
	public ResponseEntity<?> handleClassCastException(ClassCastException ex) {
		ex.printStackTrace();
		String msg = "It seems we encountered an unexpected error while processing your request";
		errorMessage.put("responsecode", "111");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<?> handleDataAccessException(DataAccessException ex) {
		ex.printStackTrace();
//		String msg = "There's a problem with accessing some data [See server logs for more details]";
		String msg = "Database error occurred. Please contact support. [See server logs for more details]";
		errorMessage.put("responsecode", "112");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex) {
		ex.printStackTrace();
		String msg = "Resources not found [See server logs for more details]";
		errorMessage.put("responsecode", "113");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<?> handleNotFoundException(NotFoundException ex) {
		ex.printStackTrace();
		String msg = "Not found";
		errorMessage.put("responsecode", "060");
		errorMessage.put("responsedesc", ex.getMessage());
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex) {
		ex.printStackTrace();
		String msg = "Unauthorized";
		errorMessage.put("responsecode", "114");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		ex.printStackTrace();
		Map<String, String> msg = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			msg.put(fieldName, errorMessage);
		});
		// String msg = "Bad request";
		errorMessage.put("responsecode", "115");
		errorMessage.put("responsedesc", msg.toString());
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(javax.security.sasl.AuthenticationException.class)
	public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
		ex.printStackTrace();
		String msg = "Authentication failed";
		errorMessage.put("responsecode", "116");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
	}

	@ExceptionHandler(AuthorizationException.class)
	public ResponseEntity<?> handleAuthorizationException(AuthorizationException ex) {
		ex.printStackTrace();
		String msg = "Authorization forbidden";
		errorMessage.put("responsecode", "117");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMessage);
	}

	@ExceptionHandler(MethodNotAllowedException.class)
	public ResponseEntity<?> handleMethodNotAllowedException(MethodNotAllowedException ex) {
		ex.printStackTrace();
		String msg = "The action you're trying to perform is not supported.";
		errorMessage.put("responsecode", "118");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorMessage);
	}

	@ExceptionHandler(ConcurrencyFailureException.class)
	public ResponseEntity<?> handleConcurrencyFailureException(ConcurrencyFailureException ex) {
		ex.printStackTrace();
		String msg = "We encountered a problem while processing your request. Concurrency failure.";
		errorMessage.put("responsecode", "119");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
	}

	@ExceptionHandler(DataNotFoundException.class)
	public ResponseEntity<?> DataNotFoundException(DataNotFoundException ex) {
		ex.printStackTrace();
		String msg = "Data not found [See logs for more details]";
		errorMessage.put("responsecode", "120");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(LockedException.class)
	public ResponseEntity<?> handleLockedException(LockedException ex) {
		ex.printStackTrace();
		String msg = "User is blocked";
		errorMessage.put("responsecode", "122");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<?> handleUsernameNotFoundException(LockedException ex) {
		ex.printStackTrace();
//		String msg = "User not found";
		errorMessage.put("responsecode", "123");
		errorMessage.put("responsedesc", ex.getMessage());
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
	}

	@ExceptionHandler(UncategorizedSQLException.class)
	public ResponseEntity<Object> handleUncategorizedSQLException(UncategorizedSQLException ex, WebRequest request) {
		ex.printStackTrace();
		String msg = "The offset specified in a OFFSET clause may not be negative [See logs for more details]";
		errorMessage.put("responsecode", "123");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);

	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<?> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
		String parameterName = ex.getParameterName();
		String msg = String.format("Required request parameter '%s' is not present", parameterName);
		errorMessage.put("responsecode", "124");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		ex.printStackTrace();
		String msg = String.format("Request method '%s' is not supported for this endpoint. Supported methods are %s.",
				ex.getMethod(), ex.getSupportedHttpMethods());
		errorMessage.put("responsecode", "125");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	@ExceptionHandler(CannotCreateTransactionException.class)
	public ResponseEntity<?> handleCannotCreateTransactionException(CannotCreateTransactionException ex,
																	WebRequest request) {
		ex.printStackTrace();
		String msg = "Unable to connect to the database. Please try again later";
		errorMessage.put("responsecode", "126");
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
//		String msg = "Data integrity violation";
		String msg = "duplicate key value violates unique constraint [See logs for more details]";
		errorMessage.put("responsecode", "127");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		ex.printStackTrace();

		String msg = "Malformed JSON request [See logs for more details]";
		errorMessage.put("responsecode", "128");
		errorMessage.put("responsedesc", msg);
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
	}

	// Handles when HikariCP can’t provide a connection in time
	@ExceptionHandler(SQLTransientConnectionException.class)
	public ResponseEntity<?> handleConnectionPoolExhaustion(SQLTransientConnectionException ex) {
		ex.printStackTrace();
		errorMessage.put("responsecode", "130");
		errorMessage.put("responsedesc", "Database is busy, please try again later.");
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorMessage);
	}

//
//	@ExceptionHandler(ResourceNotFoundException.class)
//	public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
//		String msg = "Not Found";
//		errorMessage.put("responsecode", "123");
//		errorMessage.put("responsedesc", msg);
//		errorMessage.put("responsedata", "");
//		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
//	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<?> handleAlreadyExists(ResourceAlreadyExistsException ex) {
		ex.printStackTrace();
//		String msg = "Already exists";
		errorMessage.put("responsecode", "050");
		errorMessage.put("responsedesc", ex.getMessage());
		errorMessage.put("responsedata", "");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	public static class ResourceAlreadyExistsException extends RuntimeException {
		public ResourceAlreadyExistsException(String message) {
			super(message);
		}
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

	public static class NotFoundException extends RuntimeException {
		public NotFoundException(String message) {
			super(message);
		}
	}
}
