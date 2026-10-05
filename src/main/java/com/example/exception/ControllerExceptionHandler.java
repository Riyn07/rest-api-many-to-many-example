/* 
Rest Exception Handler with Controller Advice in Spring Spring supports
exception handling by a global Exception Handler (@ExceptionHandler) with
Controller Advice (@RestControllerAdvice). The @RestControllerAdvice annotation
is a specialization of @Component annotation so that it is auto-detected via
classpath scanning. It is a kind of interceptor that surrounds the logic in our
Controllers and allows us to apply some common logic to them.

Rest Controller Advice’s methods (annotated with @ExceptionHandler) are shared
globally across multiple @Controller components to capture exceptions and
translate them to HTTP responses. The @ExceptionHandler annotation indicates
which type of Exception we want to handle. The exception instance and the
request will be injected via method arguments. 

By using two annotations together, we can:Data Management 

    - control the body of the response along with  status code 
    - handle several exceptions in the same method 
    
How about @ResponseStatus? @RestControllerAdvice annotation tells a controller that the
object returned is automatically serialized into JSON and passed to the
HttpResponse object. You only need to return 

Java  body object instead of ResponseEntity object. But the status could be
always OK (200) although the data corresponds to an exception signal (404 – Not
Found for example). @ResponseStatus can help to set the HTTP status code for the
response:

@RestControllerAdvice with @ResponseEntity
If you use @RestControllerAdvice without @ResponseBody and @ResponseStatus, 
you can return ResponseEntity object instead


*/

package com.example.exception;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.dto.response.ValidationError;
import com.example.dto.response.ValidationErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ControllerExceptionHandler {

    /**
     * Validacion fallida del cuerpo de la peticion (@Valid @RequestBody).
     * Devuelve <b>todos</b> los campos invalidos, no solo el primero.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationErrors(MethodArgumentNotValidException ex,
            WebRequest request) {

        List<ValidationError> errors = new ArrayList<>();

        for (var fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(ValidationError.of(fieldError.getField(),
                    fieldError.getDefaultMessage(), fieldError.getRejectedValue()));
        }
        // Errores de validacion a nivel de objeto (no de campo).
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.add(ValidationError.of(error.getObjectName(),
                        error.getDefaultMessage())));

        return buildBadRequest("La peticion contiene uno o mas campos invalidos", request, errors);
    }

    /**
     * Validacion fallida de parametros de metodo (@RequestParam, @PathVariable
     * con restricciones), collected desde Spring Framework 6.1.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleHandlerMethodValidation(HandlerMethodValidationException ex,
            WebRequest request) {

        List<ValidationError> errors = new ArrayList<>();

        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String field = result.getMethodParameter() != null
                    ? result.getMethodParameter().getParameterName()
                    : "";

            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(ValidationError.of(field, error.getDefaultMessage()));
            }
        }

        return buildBadRequest("Parametros de la peticion invalidos", request, errors);
    }

    /**
     * Violaciones de restricciones de Bean Validation fuera del binding
     * (@Validated a nivel de clase, por ejemplo).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {

        List<ValidationError> errors = new ArrayList<>();

        for (var violation : ex.getConstraintViolations()) {
            errors.add(ValidationError.of(String.valueOf(violation.getPropertyPath()),
                    violation.getMessage(), violation.getInvalidValue()));
        }

        return buildBadRequest("La peticion contiene uno o mas campos invalidos", request, errors);
    }

    /** JSON mal formado: llaves sin cerrar, comas sobrantes, tipos incompatibles... */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorMessage handleUnreadableBody(HttpMessageNotReadableException ex, WebRequest request) {
        return new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                new Date(),
                "El cuerpo de la peticion no es un JSON valido: " + rootCauseMessage(ex),
                request.getDescription(false));
    }

    /** Tipo incorrecto en un parametro de ruta o query, p.ej. /api/tutorials/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorMessage handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return new ErrorMessage(
                HttpStatus.BAD_REQUEST.value(),
                new Date(),
                "El valor '" + ex.getValue() + "' no es valido para el parametro '"
                        + ex.getName() + "': se esperaba un " + ex.getRequiredType().getSimpleName(),
                request.getDescription(false));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ErrorMessage invalidCredentialsException(InvalidCredentialsException ex, WebRequest request) {
        return new ErrorMessage(
                HttpStatus.UNAUTHORIZED.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
    }

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public ErrorMessage resourceConflictException(ResourceConflictException ex, WebRequest request) {
        return new ErrorMessage(
                HttpStatus.CONFLICT.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorMessage resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.NOT_FOUND.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false));

        return message;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorMessage globalExceptionHandler(Exception ex, WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                new Date(),
                ex.getMessage(),
                request.getDescription(false));

        return message;
    }

    private ValidationErrorResponse buildBadRequest(String message, WebRequest request,
            List<ValidationError> errors) {
        return new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                new Date(),
                message,
                request.getDescription(false),
                errors);
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable cause = ex;

        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause.getMessage();
    }
}