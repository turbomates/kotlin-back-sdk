package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.validation.EmptyValueConstraint
import dev.tmsoft.lib.validation.Error
import org.valiktor.ConstraintViolation
import org.valiktor.ConstraintViolationException
import org.valiktor.Validator
import org.valiktor.i18n.toMessage

suspend fun <E> validate(obj: E, block: suspend Validator<E>.(E) -> Unit): List<Error> {
    return try {
        val validator = Validator(obj).apply { block(obj) }
        if (validator.constraintViolations.isNotEmpty()) {
            throw ConstraintViolationException(validator.constraintViolations)
        }
        emptyList()
    } catch (ex: ConstraintViolationException) {
        ex.constraintViolations.toErrorsList()
    }
}

fun <E : ConstraintViolation> Set<E>.toErrorsList(): List<Error> {
    return map { constraint ->
        val message = constraint.toMessage()

        val errorMessage = when {
            message.message.isBlank() -> constraint.constraint.name
            else -> message.message
        }

        Error(
            errorMessage,
            message.property,
            if (constraint.constraint is EmptyValueConstraint) "" else message.value
        )
    }
}
