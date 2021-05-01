package misk.policy.opa

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import wisp.logging.getLogger
import javax.inject.Inject
import javax.inject.Named

class OpaPolicyEngine @Inject constructor(
  val opaApi: OpaApi,
  @Named("opa-moshi") val moshi: Moshi
) {
  val logger = getLogger<OpaPolicyEngine>()

  fun evaluate(document: String): String {
    return evaluateWithInput(document, "{\"input\":\"\"}")
  }

  fun evaluateWithInput(document: String, input: String): String {
    val response = opaApi.queryDocument(document, input).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("[${response.code()}]: ${response.errorBody()?.string()}")
    }

    return response.body()?.string() ?: "{}"
  }

  inline fun <reified T, reified R> evaulate(document: String, input: T) : R {
    val inputAdapter = moshi.adapter<Request<T>>(Types.newParameterizedType(Request::class.java, T::class.java))
    val inputString = inputAdapter.toJson(Request(input))
    val response = opaApi.queryDocument(document, inputString).execute()
    if (!response.isSuccessful) {
      throw RuntimeException("[${response.code()}]: ${response.errorBody()?.string()}")
    }

    val outputAdapter = moshi.adapter<Response<R>>(Types.newParameterizedType(Response::class.java, R::class.java))
    return outputAdapter.fromJson(response.body()?.string()!!)!!.result
  }
}

data class Response<T>(
  val decision_id: String,
  val result: T
)

data class Request<T>(
  val input: T
)
