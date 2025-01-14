package org.infinispan.remoting.transport.jgroups;

import static org.infinispan.util.logging.Log.CLUSTER;

import java.util.Set;

import org.infinispan.remoting.responses.CacheNotFoundResponse;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.transport.AbstractRequest;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.ResponseCollector;
import org.infinispan.remoting.transport.SiteAddress;
import org.infinispan.remoting.transport.impl.RequestRepository;

/**
 * Request implementation that waits for a response from a single target site.
 *
 * @author Dan Berindei
 * @since 9.1
 */
public class SingleSiteRequest<T> extends AbstractRequest<T> {
   private final String site;

   SingleSiteRequest(ResponseCollector<T> wrapper, long requestId, RequestRepository repository, String site) {
      super(requestId, wrapper, repository);
      this.site = site;
   }

   @Override
   public void onResponse(Address sender, Response response) {
      receiveResponse(sender, response);
   }

   @Override
   public boolean onNewView(Set<Address> members) {
      // Ignore cluster views.
      return false;
   }

   private void receiveResponse(Address sender, Response response) {
      try {
         // Ignore the return value, we won't receive another response
         T result;
         synchronized (responseCollector) {
            if (isDone()) {
               // CompletableFuture already completed. We can return immediately.
               return;
            }
            result = responseCollector.addResponse(sender, response);
            if (result == null) {
               result = responseCollector.finish();
            }
         }
         complete(result);
      } catch (Exception e) {
         completeExceptionally(e);
      }
   }

   @Override
   protected void onTimeout() {
      completeExceptionally(CLUSTER.requestTimedOut(requestId, site));
   }

   public void sitesUnreachable(String unreachableSite) {
      if (site.equals(unreachableSite)) {
         receiveResponse(new SiteAddress(site), CacheNotFoundResponse.INSTANCE);
      }
   }
}
