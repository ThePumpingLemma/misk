package misk.policy.opa

import com.google.inject.Provides
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import misk.client.HttpClientConfig
import misk.client.HttpClientEndpointConfig
import misk.client.HttpClientFactory
import misk.inject.KAbstractModule
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Named
import javax.inject.Singleton

class OpaModule : KAbstractModule() {
  override fun configure() {
  }

  @Provides
  fun opaApi(
    config: OpaConfig,
    httpClientFactory: HttpClientFactory,
    @Named("opa-moshi") moshi: Moshi
  ): OpaApi {
    val okHttpClient = httpClientFactory.create(
      HttpClientEndpointConfig(
        clientConfig = HttpClientConfig(
          unixSocketFile = config.unixSocket
        )
      )
    )
    val retrofit = retrofit2.Retrofit.Builder()
      .baseUrl(config.baseUrl)
      .addConverterFactory(ScalarsConverterFactory.create())
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .client(okHttpClient)
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
