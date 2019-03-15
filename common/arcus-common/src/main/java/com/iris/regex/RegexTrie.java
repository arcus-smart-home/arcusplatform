/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.regex;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

public class RegexTrie<K,V> extends AbstractMap<K,V> {
   private final Transformer<? super K> transformer;
   private Node<K,V> root;

   public RegexTrie(Transformer<? super K> transformer) {
      this(new Node<K,V>(0), transformer);
   }

   private RegexTrie(Node<K,V> root, Transformer<? super K> transformer) {
      this.root = root;
      this.transformer = transformer;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Trie API
   /////////////////////////////////////////////////////////////////////////////
   
   @Nullable
   public RegexTrie<K,V> subTrie(@Nullable K prefix) {
      if (prefix == null) {
         return null;
      }

      NybbleIterator it = transformer.transform((K)prefix);
      Node<K,V> closest = findClosest(it);
      return (closest == null || it.unfinished())
         ? null
         : new RegexTrie<K,V>(closest, transformer);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Map API
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void clear() {
      this.root = new Node<K,V>(0);
   }

   @Override
   public boolean containsKey(@Nullable Object key) {
      return get(key) != null;
   }

   @Override
   public Set<Map.Entry<K,V>> entrySet() {
      return new EntrySet<K,V>(root);
   }

   @Override
   public boolean isEmpty() {
      return root.isEmpty();
   }

   @Override
   public V put(@Nullable K key, @Nullable V value) {
      if (key == null || value == null) throw new NullPointerException();

      NybbleIterator it = transformer.transform(key);
      Node<K,V> closest = findClosest(it);
      if (closest == null) {
         throw new IllegalStateException("cannot insert key '" + key + "' into subtrie starting with prefix '" + root.key + "'");
      }

      Node<K,V> parent = closest;
      while (it.unfinished()) {
         int next = it.value();

         Node<K,V> nparent = new Node<K,V>(parent.depth + 1);
         parent.child(next, nparent);

         parent = nparent;
         it.advance();
      }

      return parent.put(key,value);
   }

   @Nullable
   @Override
   public V get(@Nullable Object key) {
      if (key == null) {
         return null;
      }

      @SuppressWarnings("unchecked")
      NybbleIterator it = transformer.transform((K)key);

      Node<K,V> closest = findClosest(it);
      return (closest == null || it.unfinished())
         ? null
         : closest.value();
   }

   @Nullable
   @Override
   public V remove(@Nullable Object key) {
      if (key == null) {
         return null;
      }

      @SuppressWarnings("unchecked")
      NybbleIterator it = transformer.transform((K)key);

      Node<K,V> closest = findClosest(it);
      return (closest == null || it.unfinished())
         ? null
         : closest.remove();
   }

   @Override
   public int size() {
      return root.size();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation Details
   /////////////////////////////////////////////////////////////////////////////
   
   @Nullable
   private Node<K,V> findClosest(NybbleIterator it) {
      if (root.depth != 0) {
         NybbleIterator prefix = transformer.transform(root.key);
         while (prefix.unfinished()) {
            if (!it.unfinished()) {
               return null;
            }

            int fnext = it.value();
            int snext = prefix.value();
            if (fnext != snext) {
               return null;
            }

            it.advance();
            prefix.advance();
         }
      }

      Node<K,V> cur = root;
      while (it.unfinished()) {
         int next = it.value();

         Node<K,V> nxt = cur.child(next);
         if (nxt == null) {
            return cur;
         }

         cur = nxt;
         it.advance();
      }

      return cur;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // API Classes
   /////////////////////////////////////////////////////////////////////////////
   
   static interface Transformer<V> {
      NybbleIterator transform(V value);
   }
   
   static interface NybbleIterator {
      boolean unfinished();
      int value();
      void advance();
   }
   
   public static enum ByteArrayTransformer implements Transformer<byte[]> {
      INSTANCE;

      @Override
      public NybbleIterator transform(byte[] value) {
         return new ByteArrayIterator(value);
      }
   }
   
   public static enum ByteBufferTransformer implements Transformer<ByteBuffer> {
      INSTANCE;

      @Override
      public NybbleIterator transform(ByteBuffer value) {
         return new ByteBufferIterator(value);
      }
   }
   
   public static enum AsciiTransformer implements Transformer<CharSequence> {
      INSTANCE;

      @Override
      public NybbleIterator transform(CharSequence value) {
         return new CharSequenceIterator(value);
      }
   }
   
   public static enum CharSequenceTransformer implements Transformer<CharSequence> {
      INSTANCE;

      @Override
      public NybbleIterator transform(CharSequence value) {
         return new CharSequenceIterator(value);
      }
   }
   
   public static enum CharacterTransformer implements Transformer<Character> {
      INSTANCE;

      @Override
      public NybbleIterator transform(Character value) {
         return new CharacterIterator(value);
      }
   }
   
   static final class ByteArrayIterator implements NybbleIterator {
      private final int end;
      private final byte[] value;
      private int nybble;

      public ByteArrayIterator(byte[] value) {
         this.value = value;
         this.nybble = 0;
         this.end = value.length << 1;
      }

      @Override
      public boolean unfinished() {
         return nybble < end;
      }

      @Override
      public int value() {
         int idx = (nybble >> 1);
         return ((nybble & 0x01) == 0)
            ? (value[idx] >> 4) & 0x0F
            : value[idx] & 0x0F;
      }

      @Override
      public void advance() {
         nybble++;
      }
   }
   
   static final class ByteBufferIterator implements NybbleIterator {
      private final int end;
      private final ByteBuffer value;
      private int nybble;

      public ByteBufferIterator(ByteBuffer value) {
         this.value = value;
         this.nybble = 0;
         this.end = value.remaining() << 1;
      }

      @Override
      public boolean unfinished() {
         return nybble < end;
      }

      @Override
      public int value() {
         int idx = (nybble >> 1);
         int val = value.get(idx);
         return ((nybble & 0x01) == 0)
            ? (val >> 4) & 0x0F
            : val & 0x0F;
      }

      @Override
      public void advance() {
         nybble++;
      }
   }
   
   static final class AsciiIterator implements NybbleIterator {
      private final int end;
      private final CharSequence value;
      private int nybble;

      public AsciiIterator(CharSequence value) {
         this.value = value;
         this.nybble = 0;
         this.end = value.length() << 1;
      }

      @Override
      public boolean unfinished() {
         return nybble < end;
      }

      @Override
      public int value() {
         int idx = (nybble >> 1);
         return ((nybble & 0x01) == 0)
            ? (value.charAt(idx) >> 4) & 0x0F
            : value.charAt(idx) & 0x0F;
      }

      @Override
      public void advance() {
         nybble++;
      }
   }
   
   static final class CharSequenceIterator implements NybbleIterator {
      private final int end;
      private final CharSequence value;
      private int nybble;

      public CharSequenceIterator(CharSequence value) {
         this.value = value;
         this.nybble = 0;
         this.end = value.length() << 2;
      }

      @Override
      public boolean unfinished() {
         return nybble < end;
      }

      @Override
      public int value() {
         int idx = (nybble >> 2);
         switch (nybble & 0x03) {
         case 0:  return (value.charAt(idx) >> 12) & 0x0F;
         case 1:  return (value.charAt(idx) >> 8) & 0x0F;
         case 2:  return (value.charAt(idx) >> 4) & 0x0F;
         case 3:  return value.charAt(idx) & 0x0F;
         default: throw new IllegalStateException();
         }
      }

      @Override
      public void advance() {
         nybble++;
      }
   }
   
   static final class CharacterIterator implements NybbleIterator {
      private final Character value;
      private int nybble;

      public CharacterIterator(Character value) {
         this.value = value;
         this.nybble = 0;
      }

      @Override
      public boolean unfinished() {
         return nybble < 4;
      }

      @Override
      public int value() {
         switch (nybble & 0x03) {
         case 0:  return (value >> 12) & 0x0F;
         case 1:  return (value >> 8) & 0x0F;
         case 2:  return (value >> 4) & 0x0F;
         case 3:  return value & 0x0F;
         default: throw new IllegalStateException();
         }
      }

      @Override
      public void advance() {
         nybble++;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility Classes
   /////////////////////////////////////////////////////////////////////////////
   
   private static final class Node<K,V> {
      private final Node<K,V>[] children;
      private final int depth;
      private @Nullable K key;
      private @Nullable V value;

      @SuppressWarnings("unchecked")
      private Node(int depth) {
         this(new Node[16], depth);
      }

      private Node(Node<K,V>[] children, int depth) {
         this.key = null;
         this.children = children;
         this.value = null;
         this.depth = depth;
      }

      @Nullable
      V value() {
         return value;
      }

      @Nullable
      Node<K,V> child(int idx) {
         return children[idx];
      }

      Node<K,V> child(int idx, Node<K,V> child) {
         Node<K,V> result = children[idx];
         children[idx] = child;
         return result;
      }

      @Nullable
      V put(K key, V value) {
         V result = this.value;
         this.key = key;
         this.value = value;
         return result;
      }

      @Nullable
      V remove() {
         V result = this.value;
         this.value = null;
         return result;
      }

      boolean isEmpty() {
         return value == null &&
            (children[0] == null || children[0].isEmpty()) &&
            (children[1] == null || children[1].isEmpty()) &&
            (children[2] == null || children[2].isEmpty()) &&
            (children[3] == null || children[3].isEmpty()) &&
            (children[4] == null || children[4].isEmpty()) &&
            (children[5] == null || children[5].isEmpty()) &&
            (children[6] == null || children[6].isEmpty()) &&
            (children[7] == null || children[7].isEmpty()) &&
            (children[8] == null || children[8].isEmpty()) &&
            (children[9] == null || children[9].isEmpty()) &&
            (children[10] == null || children[10].isEmpty()) &&
            (children[11] == null || children[11].isEmpty()) &&
            (children[12] == null || children[12].isEmpty()) &&
            (children[13] == null || children[13].isEmpty()) &&
            (children[14] == null || children[14].isEmpty()) &&
            (children[15] == null || children[15].isEmpty());
      }

      int size() {
         int subsize = 0;
         for (int i = 0; i < 16; ++i) {
            if (children[i] != null) {
               subsize += children[i].size();
            }
         }

         return (value == null) ? subsize : subsize + 1;
      }

      @Override
      @SuppressWarnings("null")
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + Arrays.hashCode(children);
         result = prime * result + ((value == null) ? 0 : value.hashCode());
         return result;
      }

      @Override
      @SuppressWarnings({"null","rawtypes", "unused"})
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         Node other = (Node) obj;
         if (!Arrays.equals(children, other.children))
            return false;
         if (value == null) {
            if (other.value != null)
               return false;
         } else if (!value.equals(other.value))
            return false;
         return true;
      }
   }

   private static final class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>> {
      private final Node<K,V> delegate;

      public EntrySet(Node<K,V> delegate) {
         this.delegate = delegate;
      }

      @Override
      public int size() {
         return delegate.size();
      }

      @Override
      public Iterator<Map.Entry<K,V>> iterator() {
         return new EntrySetIterator<K,V>(delegate);
      }
   }

   private static final class EntrySetIterator<K,V> implements Iterator<Map.Entry<K,V>> {
      private List<EntrySetLocation<K,V>> stack;
      private @Nullable Node<K,V> last;

      public EntrySetIterator(Node<K,V> root) {
         this.stack = new ArrayList<>();

         EntrySetLocation<K,V> loc = new EntrySetLocation<>(root);
         this.stack.add(loc);
         
         if (root.value == null) {
            advanceToNext(loc);
         }
      }

      @Override
      public boolean hasNext() {
         return top() != null;
      }

      @Override
      public Map.Entry<K,V> next() {
         EntrySetLocation<K,V> n = top();
         if (n == null) {
            throw new NoSuchElementException();
         }

         advanceToNext(n);
         last = n.node;

         return new AbstractMap.SimpleImmutableEntry<K,V>(n.node.key, n.node.value);
      }

      @Override
      public void remove() {
         Node<K,V> lst = last;
         last = null;

         if (lst == null) {
            throw new IllegalStateException();
         }

         lst.remove();
      }

      @Nullable
      private EntrySetLocation<K,V> top() {
         if (stack.isEmpty()) return null;
         return stack.get(stack.size() - 1);
      }

      @Nullable
      private EntrySetLocation<K,V> pop() {
         if (!stack.isEmpty()) {
            stack.remove(stack.size() - 1);
         }

         return top();
      }

      private void advanceToNext(EntrySetLocation<K,V> top) {
         EntrySetLocation<K,V> cur = top;
         while (cur != null) {
            while (cur.idx < 16) {
               Node<K,V> child = cur.node.children[cur.idx];
               cur.idx++;

               if (child != null) {
                  cur = new EntrySetLocation<>(child);
                  stack.add(cur);

                  if (child.value != null) {
                     return;
                  }
               }
            }

            cur = pop();
         }
      }
   }

   private static final class EntrySetLocation<K,V> {
      private final Node<K,V> node;
      private int idx;

      public EntrySetLocation(Node<K, V> node) {
         this.node = node;
         this.idx = 0;
      }
   }
}

