package dev.tmsoft.lib.ktor

import dev.tmsoft.lib.extension.camelToSnakeCase
import dev.tmsoft.lib.validation.EmptyValueConstraint
import dev.tmsoft.lib.validation.Error
import org.valiktor.ConstraintViolation
import org.valiktor.ConstraintViolationException
import org.valiktor.Validator
import org.valiktor.i18n.toMessage

/**
 * Validate object without namespace
 *
 * @param obj Object to validate
 * @param block Validation block
 */
suspend fun <E> validate(obj: E, block: suspend Validator<E>.(E) -> Unit): List<Error> {
    return validate(obj, "", block)
}

/**
 * Validate object with form-level namespace
 *
 * @param obj Object to validate
 * @param namespace Form-level namespace (e.g., "bonus.update_achievement")
 * @param block Validation block
 */
suspend fun <E> validate(
    obj: E,
    namespace: String,
    block: suspend Validator<E>.(E) -> Unit
): List<Error> {
    return try {
        val validator = Validator(obj)
        validator.block(obj)
        if (validator.constraintViolations.isNotEmpty()) {
            throw ConstraintViolationException(validator.constraintViolations)
        }
        emptyList()
    } catch (ex: ConstraintViolationException) {
        ex.constraintViolations.toErrorsList(namespace)
    }
}

/**
 * Convert constraint violations to error list
 *
 * @param formNamespace Form-level namespace prefix
 * @return List of errors with namespace set to formNamespace.property_snake_case
 */
fun Set<ConstraintViolation>.toErrorsList(formNamespace: String = ""): List<Error> {
    return map { violation ->
        val message = violation.toMessage()
        val constraintName = message.constraint.name
        val propertySnakeCase = message.property.camelToSnakeCase()

        val key = if (constraintName.contains('.')) {
            constraintName
        } else {
            "validation.${constraintName.camelToSnakeCase()}"
        }

        val namespace = if (formNamespace.isNotEmpty()) {
            "$formNamespace.$propertySnakeCase"
        } else {
            propertySnakeCase
        }

        Error(
            key = key,
            namespace = namespace,
            property = message.property,
            value = if (violation.constraint is EmptyValueConstraint) "" else message.value,
            parameters = violation.constraint.messageParams
        )
    }
}
