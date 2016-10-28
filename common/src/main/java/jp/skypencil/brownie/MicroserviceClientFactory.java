package jp.skypencil.brownie;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Single;

/**
 * A factory class which generates {@code Single<HttpClient>} to connect to
 * other microservices. Caller should hold an instance of this factory as
 * instance field, and call {@link #createClient(String, Future)} when it tries
 * to call other microservices.
 */
@ParametersAreNonnullByDefault
@RequiredArgsConstructor(
        onConstructor = @__(@Inject))
public class MicroserviceClientFactory {
    private final ServiceDiscovery discovery;

    /**
     * @param name
     *            Name of target microservice.
     * @param closed
     *            A {@link Future} which should be completed when caller finished using returned {@link HttpClient}.
     * @return A {@link Single} which emits a {@link HttpClient} to invoke other
     *         microservice. Caller does not have to close this client, but it
     *         should complete {@link Future} when it finishes to use returned {@link HttpClient}.
     */
    @Nonnull
    public Single<HttpClient> createClient(String name, Future<Void> closed) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(closed, "closed");
        if (closed.isComplete()) {
            return Single.error(new IllegalArgumentException("Given Future is already closed"));
        }

        return discovery
                .getRecordObservable(new JsonObject().put("name", name))
                .map(discovery::getReference)
                .flatMap(reference -> {
                    HttpClient client = new HttpClient(reference.get());
                    closed.setHandler(ar -> {
                        // this will invoke HttpEndpointReference#close() which closes HttpClient,
                        // so we do not have to call client.close() explicitly.
                        reference.release();
                    });
                    return Observable.just(client);
                })
                .toSingle();
    }
}
