package jp.skypencil.brownie;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.rxjava.servicediscovery.ServiceReference;
import io.vertx.rxjava.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;

@RunWith(VertxUnitRunner.class)
public class MicroserviceClientFactoryTest {
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Test
    public void testSucceededFuture(TestContext context) {
        Vertx vertx = Vertx.newInstance(rule.vertx());
        ServiceDiscovery discovery = ServiceDiscovery.create(vertx);
        MicroserviceClientFactory factory = new MicroserviceClientFactory(discovery);

        Async async = context.async();
        factory.createClient("name", Future.succeededFuture())
            .subscribe(client -> {
                context.fail();
            }, e -> {
                assertThat(e).isInstanceOf(IllegalArgumentException.class);
                async.complete();
            });
    }

    @Test
    public void ensureClientIsCreatedBasedOnServiceDiscovery(TestContext context) {
        Vertx vertx = Vertx.newInstance(rule.vertx());
        ServiceDiscovery discovery = spy(ServiceDiscovery.create(vertx));
        Async async = context.async();

        discovery.publish(HttpEndpoint.createRecord("name", "localhost"), ar -> {
            assertThat(ar.succeeded()).isTrue();

            MicroserviceClientFactory factory = new MicroserviceClientFactory(discovery);
            Future<Void> future = spy(Future.future());

            factory.createClient("name", future).subscribe(client -> {
                verify(discovery).getRecordObservable(eq(new JsonObject().put("name", "name")));
                async.complete();
            }, context::fail);
        });
    }

    @Test
    public void ensureReferenceIsReleased(TestContext context) {
        Vertx vertx = Vertx.newInstance(rule.vertx());
        ServiceDiscovery discovery = spy(ServiceDiscovery.create(vertx));
        Async async = context.async();

        discovery.publish(HttpEndpoint.createRecord("name", "localhost"), ar -> {
            assertThat(ar.succeeded()).isTrue();

            MicroserviceClientFactory factory = new MicroserviceClientFactory(discovery);
            Future<Void> future = Future.future();
            AtomicReference<ServiceReference> reference = new AtomicReference<>();

            doAnswer(invocation -> {
                reference.set(spy((ServiceReference) invocation.callRealMethod()));
                return reference.get();
            }).when(discovery).getReference(any(Record.class));

            factory.createClient("name", future).subscribe(client -> {
                verify(reference.get(), never()).release();

                future.complete();
                verify(reference.get()).release();

                async.complete();
            }, context::fail);
        });
    }

}
