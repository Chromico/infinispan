package org.infinispan.distribution.group.impl;

import static org.infinispan.commons.util.ReflectionUtil.invokeMethod;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.commons.util.ReflectionUtil;
import org.infinispan.commons.util.Util;
import org.infinispan.distribution.group.Group;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;

@Scope(Scopes.NAMED_CACHE)
public class GroupManagerImpl implements GroupManager {

   @Inject ComponentRegistry componentRegistry;

   private final ConcurrentMap<Class<?>, GroupMetadata> groupMetadataCache;
   private final List<Grouper<?>> groupers;

   public GroupManagerImpl(List<Grouper<?>> groupers) {
      this.groupMetadataCache = new ConcurrentHashMap<>();
      if (groupers != null)
         this.groupers = groupers;
      else
         this.groupers = Collections.emptyList();
   }

   @Override
   public Object getGroup(Object key) {
      GroupMetadata metadata = getMetadata(key);
      if (metadata != null) {
         return applyGroupers(metadata.getGroup(key), key);
      } else
         return applyGroupers(null, key);
   }

   private interface GroupMetadata {
      GroupMetadata NONE = instance -> null;

      Object getGroup(Object instance);
   }

   private static class GroupMetadataImpl implements GroupMetadata {
      private final Method method;

      GroupMetadataImpl(Method method) {
         if (method.getParameterCount() > 0)
            throw new IllegalArgumentException(Util.formatString("@Group method %s must have zero arguments", method));
         this.method = method;
      }

      @Override
      public Object getGroup(Object instance) {
         if (System.getSecurityManager() == null) {
            method.setAccessible(true);
         } else {
            AccessController.doPrivileged((PrivilegedAction<List<Method>>) () -> {
               method.setAccessible(true);
               return null;
            });
         }
         return invokeMethod(instance, method, Util.EMPTY_OBJECT_ARRAY);
      }
   }

   private static GroupMetadata createGroupMetadata(Class<?> clazz) {
      Collection<Method> possibleMethods;
      if (System.getSecurityManager() == null) {
         possibleMethods = ReflectionUtil.getAllMethods(clazz, Group.class);
      } else {
         possibleMethods = AccessController.doPrivileged((PrivilegedAction<List<Method>>) () -> ReflectionUtil.getAllMethods(clazz, Group.class));
      }
      if (possibleMethods.isEmpty())
         return GroupMetadata.NONE;
      else if (possibleMethods.size() == 1)
         return new GroupMetadataImpl(possibleMethods.iterator().next());
      else
         throw new IllegalStateException(Util.formatString("Cannot define more that one @Group method for class hierarchy rooted at %s", clazz.getName()));
   }

   private Object applyGroupers(Object group, Object key) {
      for (Grouper<?> grouper : groupers) {
         if (grouper.getKeyType().isAssignableFrom(key.getClass()))
             group = ((Grouper<Object>) grouper).computeGroup(key, group);
      }
      return group;
   }

   private GroupMetadata getMetadata(final Object key) {
      final Class<?> keyClass = key.getClass();
      GroupMetadata groupMetadata = groupMetadataCache.get(keyClass);
      if (groupMetadata == null) {
         //this is not ideal as it is possible for the group metadata to be redundantly calculated several times.
         //however profiling showed that using the Map<Class,Future> cache-approach is significantly slower on
         // the long run
         groupMetadata = createGroupMetadata(keyClass);
         GroupMetadata previous = groupMetadataCache.putIfAbsent(keyClass, groupMetadata);
         if (previous != null) {
            // in case another thread added a metadata already, discard what we created and reuse the existing.
            return previous;
         }
      }
      return groupMetadata;
   }
}
