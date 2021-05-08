package misk.policy.opa

import com.google.inject.Provides
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import misk.client.HttpClientFactory
import misk.inject.KAbstractModule
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Named
import javax.inject.Singleton

class OpaModule : KAbstractModule() {
  override fun configure() {
  }

  @Provides
  internal fun opaApi(
    config: OpaConfig,
    httpClientFactory: HttpClientFactory,
    @Named("opa-moshi") moshi: Moshi
  ): OpaApi {
    val retrofit = retrofit2.Retrofit.Builder()
      .addConverterFactory(ScalarsConverterFactory.create())
      .baseUrl(config.baseUrl)
      .build()
    return retrofit.create(OpaApi::class.java)
  }

  @Provides @Singleton @Named("opa-moshi")
  fun provideMoshi(): Moshi {
    return Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
  }
}
