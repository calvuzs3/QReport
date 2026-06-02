package net.calvuz.qreport.client.client.domain.validator

import net.calvuz.qreport.app.error.domain.model.QrError
import net.calvuz.qreport.app.result.domain.QrResult
import net.calvuz.qreport.client.client.domain.model.Client
import javax.inject.Inject

/**
 * Validates [Client] domain data before create/update operations.
 *
 * Returns [QrResult.Success] if all rules pass, or [QrResult.Error] with a
 * [QrError.ClientError] describing the first failing rule.
 *
 * Validation rules:
 * - [Client.id] must not be blank
 * - [Client.companyName] must not be blank
 * - [Client.companyName] must be at least 2 characters
 * - [Client.companyName] must be at most 255 characters
 */
class ClientDataValidator @Inject constructor() {

    operator fun invoke(client: Client): QrResult<Unit, QrError.ClientError> = when {
        client.id.isBlank() ->
            QrResult.Error(QrError.ClientError.MissingCompanyName())

        client.companyName.isBlank() ->
            QrResult.Error(QrError.ClientError.MissingCompanyName())

        client.companyName.length < 2 ->
            QrResult.Error(QrError.ClientError.InvalidCompanyName())

        client.companyName.length > 255 ->
            QrResult.Error(QrError.ClientError.InvalidCompanyName())

        else -> QrResult.Success(Unit)
    }
}