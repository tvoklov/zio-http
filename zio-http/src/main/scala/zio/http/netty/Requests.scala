package zio.http.netty

import io.netty.channel._
import io.netty.handler.codec.http.{FullHttpRequest, HttpRequest}
import zio._
import zio.http._

import java.net.InetSocketAddress

private[zio] object Requests {

  def make(nettyReq: HttpRequest, ctx: ChannelHandlerContext)(implicit unsafe: Unsafe): Request = {
    val protocolVersion = Versions.make(nettyReq.protocolVersion())

    val remoteAddress = ctx.channel().remoteAddress() match {
      case m: InetSocketAddress => Some(m.getAddress)
      case _                    => None
    }

    nettyReq match {
      case nettyReq: FullHttpRequest =>
        //   override final val unsafe: UnsafeAPI = new UnsafeAPI {
        //     override final def encode(implicit unsafe: Unsafe): HttpRequest = jReq
        //     override final def context(implicit unsafe: Unsafe): Ctx        = ctx
        //   }
        Request(
          Body.fromByteBuf(nettyReq.content()),
          Headers.make(nettyReq.headers()),
          Method.fromHttpMethod(nettyReq.method()),
          URL.fromString(nettyReq.uri()).getOrElse(URL.empty),
          protocolVersion,
          remoteAddress,
        )
      case nettyReq: HttpRequest     =>
        //   override final val unsafe: UnsafeAPI = new UnsafeAPI {
        //     override final def encode(implicit unsafe: Unsafe): HttpRequest = jReq

        //     override final def context(implicit unsafe: Unsafe): Ctx = ctx
        //   }
        val body = Body.fromAsync { async =>
          ctx.addAsyncBodyHandler(async)
        }
        Request(
          body,
          Headers.make(nettyReq.headers()),
          Method.fromHttpMethod(nettyReq.method()),
          URL.fromString(nettyReq.uri()).getOrElse(URL.empty),
          protocolVersion,
          remoteAddress,
        )
    }

  }
}