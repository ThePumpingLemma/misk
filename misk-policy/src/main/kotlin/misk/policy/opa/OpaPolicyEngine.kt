package misk.policy.opa

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Named

/**
 * Support for the Open Policy Engine (OPA).
 * OPA provides a means to decouple policy from business logic. The resulting query response may
 * have arbitrary shapes.
 */
class OpaPolicyEngine @Inject constructor(
  val opaApi: OpaApi,
  @Named("opa-moshi") val moshi: Moshi
) {

  /**
   * Evaluate / Query a document with given input of shape T.
   * This will connect to OPA via a retrofit interface and perform a /v1/data/{document} POST.
   *
   * @throws RuntimeException if the request to OPA failed or the response shape didn't match R.
   * @return Response shape R from OPA.
   */
  inline fun <reified T, reified R> evaluate(document: String, input: T): R {
    val inputAdapter = moshi.adapter<Request<T>>(
      Types.newParameterizedType(Request::class.java, T::class.java)
    )
    val inputString = inputAdapter.toJson(Request(input))
    val response = opaApi.queryDocument(document, inputString).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("[${response.code()}]: ${response.errorBody()?.string()}")
    }

    val outputAdapter = moshi.adapter<Response<R>>(
      Types.newParameterizedType(Response::class.java, R::class.java)
    )

    val responseBody =
      response.body()?.string() ?: throw RuntimeException("OPA response body is empty")
    val extractedResponse = try {
      outputAdapter.fromJson(responseBody)
        ?: throw RuntimeException("Unmarshalled OPA response body is empty")
    } catch(e: Exception) {
      throw RuntimeException("Response shape did not match", e)
    }

    return extractedResponse.result
  }

  /**
   * Evaluate / Query a document with no additional input.
   * This will connect to OPA via a retrofit interface and perform a /v1/data/{document} POST.
   *
   * @throws RuntimeException if the request to OPA failed or the response shape didn't match R.
   * @return Response shape R from OPA.
   */
  inline fun <reified R> evaluate(document: String): R {
    val response = opaApi.queryDocument(document).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("[${response.code()}]: ${response.errorBody()?.string()}")
    }

    val outputAdapter = moshi.adapter<Response<R>>(
      Types.newParameterizedType(Response::class.java, R::class.java)
    )

    val responseBody =
      response.body()?.string() ?: throw RuntimeException("OPA response body is empty")
    val extractedResponse = outputAdapter.fromJson(responseBody)
      ?: throw RuntimeException("Unmarshalled OPA response body is empty")

    return extractedResponse.result
  }
}
