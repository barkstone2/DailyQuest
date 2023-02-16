package todayquest.exception

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import todayquest.common.MessageUtil
import todayquest.common.ResponseData
import java.util.function.Consumer

@RestControllerAdvice
class RestApiExceptionHandler {

    var log = LoggerFactory.getLogger(this.javaClass)

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(
        NoHandlerFoundException::class
    )
    fun notFound(): ResponseData<Void> {
        val errorResponse = ErrorResponse(MessageUtil.getMessage("exception.notFound"), HttpStatus.NOT_FOUND)
        return ResponseData(errorResponse)
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(
        AccessDeniedException::class
    )
    fun accessDenied(e: IllegalArgumentException): ResponseData<Void> {
        val errorResponse = ErrorResponse(e.message, HttpStatus.FORBIDDEN)
        return ResponseData(errorResponse)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(
        IllegalArgumentException::class
    )
    fun illegalExHandle(e: IllegalArgumentException): ResponseData<Void> {
        log.error("[exceptionHandle] ex", e)
        val errorResponse = ErrorResponse(e.message, HttpStatus.BAD_REQUEST)
        return ResponseData(errorResponse)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(
        HttpMessageNotReadableException::class,
        HttpMessageConversionException::class,
        BindException::class,
        MethodArgumentTypeMismatchException::class,
        HttpMediaTypeNotSupportedException::class,
        ConstraintViolationException::class,
    )
    fun badRequest(e: Exception): ResponseData<Void> {
        log.error("[exceptionHandle]", e)
        val errorResponse = ErrorResponse(MessageUtil.getMessage("exception.badRequest"), HttpStatus.BAD_REQUEST)
        return ResponseData(errorResponse)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
    )
    fun bindingResultError(e: MethodArgumentNotValidException): ResponseData<Void> {
        return handleBindingResult(e.bindingResult)
    }

    private fun <T> handleBindingResult(bindingResult: BindingResult): ResponseData<T> {
        val fieldErrors = bindingResult.fieldErrors
        val errorResponse = ErrorResponse(MessageUtil.getMessage("exception.badRequest"), HttpStatus.BAD_REQUEST)
        fieldErrors.forEach(Consumer { fieldError: FieldError ->
            errorResponse.errors.add(fieldError.field, fieldError.defaultMessage)
        })
        return ResponseData(errorResponse)
    }

}