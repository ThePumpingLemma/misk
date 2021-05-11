package misk.policy.opa

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Named

/**
 * Support for the Open Policy Engine (OPA).
 * OPA provides a means to decouple policy from business logic. The resulting query response may
 * have arbitrary shapes.
 */
class OpaPolicyEngine @Inject constructor(
  private val opaApi: OpaApi,
  @Named("opa-moshi") private val moshi: Moshi
) {

  /**
   * Evaluate / Query a document with given input of shape T.
   * This will connect to OPA via a retrofit interface and perform a /v1/data/{document} POST.
   *
   * @throws PolicyEngineException if the request to OPA failed or the response shape didn't match R.
   * @return Response shape R from OPA.
   */
  fun <T, R> evaluateInternal(
    document: String,
    input: T,
    inputType: Class<T>,
    returnType: Class<R>
  ): R {
    val inputAdapter = moshi.adapter<Request<T>>(
      Types.newParameterizedType(Request::class.java, inputType)
    )
    val inputString = inputAdapter.toJson(Request(input))
    val response = queryOpa(document, inputString)
    return parseResponse(returnType, response)
  }

  inline fun <reified T, reified R> evaluate(document: String, input: T): R {
    return evaluateInternal(document, input, T::class.java, R::class.java)
  }

  /**
   * Evaluate / Query a document with no additional input.
   * This will connect to OPA via a retrofit interface and perform a /v1/data/{document} POST.
   *
   * @throws PolicyEngineException if the request to OPA failed or the response shape didn't match R.
   * @return Response shape R from OPA.
   */
  fun <R> evaluateInternal(document: String, returnType: Class<R>): R {
    val response = queryOpa(document)
    return parseResponse(returnType, response)
  }

  private fun queryOpa(
    document: String,
    inputString: String? = null
  ): retrofit2.Response<ResponseBody> {
    val response = when (inputString) {
      null -> opaApi.queryDocument(document).execute()
      else -> opaApi.queryDocument(document, inputString).execute()
    }
    if (!response.isSuccessful) {
      throw PolicyEngineException("[${response.code()}]: ${response.errorBody()?.string()}")
    }
    return response
  }

  inline fun <reified R> evaluate(document: String): R {
    return evaluateInternal(document, R::class.java)
  }

  private fun <R> parseResponse(
    returnType: Class<R>,
    response: retrofit2.Response<ResponseBody>
  ): R {
    val outputAdapter = moshi.adapter<Response<R>>(
      Types.newParameterizedType(Response::class.java, returnType)
    )

    val responseBody =
      response.body()?.string() ?: throw PolicyEngineException("OPA response body is empty")
    val extractedResponse = try {
      outputAdapter.fromJson(responseBody)
        ?: throw PolicyEngineException("Unmarshalled OPA response body is empty")
    } catch (e: Exception) {
      throw PolicyEngineException("Response shape did not match", e)
    }

    return extractedResponse.result
  }
}
