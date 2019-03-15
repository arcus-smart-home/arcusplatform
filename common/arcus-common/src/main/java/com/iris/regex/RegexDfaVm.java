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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

public class RegexDfaVm<V> {
   private static final int CONSUME_INSTRUCTION = 0x10000000;
   private static final int MATCH_INSTRUCTION   = 0x20000000;
   private static final int JUMP_INSTRUCTION    = 0x30000000;
   private static final int SPLIT_INSTRUCTION   = 0x40000000;

   private static final int INSTR_MASK          = 0x70000000;

   private final int[] instrs;

   private RegexDfaVm(int[] instrs) {
      this.instrs = instrs;
   }

   public static <V> Builder<V> builder() {
      return new Builder<>();
   }

   public Matcher matcher() {
      return new Matcher();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Matching over byte arrays
   /////////////////////////////////////////////////////////////////////////////

   public boolean matches(byte[] input) {
      Matcher match = matcher();
      for (int i = 0, e = input.length; i < e; ++i) {
         if (match.process(input[i])) {
            return false;
         }
      }

      match.finish();
      return match.matched();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Matching over byte iterators
   /////////////////////////////////////////////////////////////////////////////

   public boolean matches(Iterable<Byte> input) {
      return matches(input.iterator());
   }

   public boolean matches(Iterator<Byte> input) {
      Matcher match = matcher();
      while (input.hasNext()) {
         if (match.process(input.next())) {
            return false;
         }
      }

      match.finish();
      return match.matched();
   }

   @Nullable
   public V matching(Iterable<Byte> input) {
      return matching(input.iterator());
   }

   @Nullable
   public V matching(Iterator<Byte> input) {
      Matcher match = matcher();
      while (input.hasNext()) {
         if (match.process(input.next())) {
            return null;
         }
      }

      match.finish();
      return match.match();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////
   
   private static int dopack(int instr, int v1, int v2) {
      if ((v1 & (~0x3FFF)) != 0) {
         throw new IllegalStateException("cannot compile regex");
      }

      if ((v2 & (~0x3FFF)) != 0) {
         throw new IllegalStateException("cannot compile regex");
      }

      return instr | ((v1 & 0x3FFF) << 14) | (v2 & 0x3FFF);
   }

   public final class Matcher {
      private int[] cpcs;
      private int[] npcs;

      int ncur;
      int nnxt;

      public Matcher() {
         this.cpcs = new int[instrs.length];
         this.npcs = new int[instrs.length];

         this.ncur = 1;
         this.nnxt = 0;
      }

      public void finish() {
         for (int i = 0; i < ncur; ++i) {
            if (cpcs[i] >= instrs.length) {
               continue;
            }

            while (true) {
               int ins = instrs[cpcs[i]];
               int op = ins & INSTR_MASK;

               if (op == CONSUME_INSTRUCTION) {
                  break;
               } else if (op == MATCH_INSTRUCTION) {
                  npcs[nnxt++] = cpcs[i];
                  break;
               } else if (op == JUMP_INSTRUCTION) {
                  cpcs[i] = (int)(ins & 0x3FFF);
               } else if (op == SPLIT_INSTRUCTION) {
                  int spc = (int)((ins >> 14) & 0x3FFF);
                  cpcs[ncur++] = spc;
                  cpcs[i] = (int)(ins & 0x3FFF);
               } else {
                  throw new IllegalStateException("unknown opcode: " + Long.toHexString(op));
               }
            }
         }

         int[] tmp = cpcs;
         cpcs = npcs;
         npcs = tmp;

         ncur = nnxt;
         nnxt = 0;
      }

      public boolean process(byte symbol) {
         for (int i = 0; i < ncur; ++i) {
            if (cpcs[i] >= instrs.length) {
               continue;
            }

            while (true) {
               int ins = instrs[cpcs[i]];
               int op = ins & INSTR_MASK;

               if (op == CONSUME_INSTRUCTION) {
                  int csym = ins & 0xFF;
                  if (csym == (symbol & 0xFF)) {
                     int npc = cpcs[i] + 1;
                     for (int c=0; c<nnxt; ++c) {
                        if (npcs[c] == npc) {
                           break;
                        }
                     }
                     npcs[nnxt++] = cpcs[i] + 1;
                  }
                  break;
               } else if (op == MATCH_INSTRUCTION) {
                  cpcs[i]++;
                  if (cpcs[i] >= instrs.length) {
                     break;
                  }
               } else if (op == JUMP_INSTRUCTION) {
                  cpcs[i] = ins & 0x3FFF;
               } else if (op == SPLIT_INSTRUCTION) {
                  cpcs[ncur++] = (ins >> 14) & 0x3FFF;
                  cpcs[i] = ins & 0x3FFF;
               }
            }
         }

         int[] tmp = cpcs;
         cpcs = npcs;
         npcs = tmp;

         ncur = nnxt;
         nnxt = 0;

         return ncur == 0;
      }

      public boolean matched() {
         return ncur > 0;
      }

      @Nullable
      public V match() {
         return null;
      }
   }

   public static interface Instruction<V> {
      boolean isFinal();
      int pack(LocationResolver<V> resolv);
   }

   public static final class ConsumeInstruction<V> implements Instruction<V> {
      private final byte symbol;

      public ConsumeInstruction(byte symbol) {
         this.symbol = symbol;
      }

      @Override
      public boolean isFinal() {
         return false;
      }

      @Override
      public int pack(LocationResolver<V> resolv) {
         return dopack(CONSUME_INSTRUCTION, 0, symbol & 0xFF);
      }
   }

   public static final class MatchInstruction<V> implements Instruction<V> {
      private final @Nullable V value;

      public MatchInstruction() {
         this.value = null;
      }

      public MatchInstruction(V value) {
         this.value = value;
      }

      @Override
      public boolean isFinal() {
         return true;
      }

      @Override
      public int pack(LocationResolver<V> resolv) {
         return dopack(MATCH_INSTRUCTION,0,0);
      }
   }

   public static final class JumpInstruction<V> implements Instruction<V> {
      private final BasicBlock<V> blk;

      public JumpInstruction(BasicBlock<V> blk) {
         this.blk = blk;
      }

      @Override
      public boolean isFinal() {
         return true;
      }

      @Override
      public int pack(LocationResolver<V> resolv) {
         return dopack(JUMP_INSTRUCTION, 0, resolv.resolve(blk));
      }
   }

   public static final class SplitInstruction<V> implements Instruction<V> {
      private final BasicBlock<V> blk1;
      private final BasicBlock<V> blk2;

      public SplitInstruction(BasicBlock<V> blk1, BasicBlock<V> blk2) {
         this.blk1 = blk1;
         this.blk2 = blk2;
      }

      @Override
      public boolean isFinal() {
         return true;
      }

      @Override
      public int pack(LocationResolver<V> resolv) {
         return dopack(SPLIT_INSTRUCTION, resolv.resolve(blk1), resolv.resolve(blk2));
      }
   }

   public static final class BasicBlock<V> {
      private final List<Instruction<V>> instrs;

      private BasicBlock() {
         this.instrs = new ArrayList<>();
      }

      public BasicBlock<V> consume(byte symbol) {
         instrs.add(new ConsumeInstruction<V>(symbol));
         return this;
      }

      public void match() {
         instrs.add(new MatchInstruction<V>());
      }

      public void match(V value) {
         instrs.add(new MatchInstruction<V>(value));
      }

      public void jump(BasicBlock<V> blk) {
         instrs.add(new JumpInstruction<V>(blk));
      }

      public void split(BasicBlock<V> blk1, BasicBlock<V> blk2) {
         instrs.add(new SplitInstruction<V>(blk1,blk2));
      }

      public int size() {
         return instrs.size();
      }

      public void verify() {
         Instruction<V> last = null;
         for (Instruction<V> ins : instrs) {
            if (last != null && last.isFinal()) {
               throw new IllegalStateException("basic block contains a final instruction at a non-end position");
            }

            last = ins;
         }

         if (last != null && !last.isFinal()) {
            throw new IllegalStateException("basic block is not terminated by a final instruction");
         }
      }
   }

   public static interface LocationResolver<V> {
      int resolve(BasicBlock<V> blk);
   }

   public static final class MapBasedLocationResolver<V> implements LocationResolver<V> {
      private final Map<BasicBlock<V>, Integer> locations;

      public MapBasedLocationResolver(Map<BasicBlock<V>, Integer> locations) {
         this.locations = locations;
      }

      @Override
      public int resolve(BasicBlock<V> blk) {
         Integer result = locations.get(blk);
         if (result == null) {
            throw new IllegalStateException("could not resolve block location for block that was not compiled");
         }

         return result;
      }
   }

   public static final class Builder<V> {
      private List<BasicBlock<V>> blocks;

      public Builder() {
         this.blocks = new ArrayList<>();
      }

      public BasicBlock<V> block() {
         BasicBlock<V> result = new BasicBlock<V>();
         blocks.add(result);
         return result;
      }

      public RegexDfaVm<V> build() {
         int size = 0;
         for (BasicBlock<V> blk : blocks) {
            blk.verify();
            size += blk.size();
         }

         int pc = 0;
         Map<BasicBlock<V>,Integer> blockPlacement = new IdentityHashMap<>();

         int[] instrs = new int[size];
         for (BasicBlock<V> blk : blocks) {
            blockPlacement.put(blk,pc);
            pc += blk.instrs.size();
         }

         pc = 0;
         LocationResolver<V> resolver = new MapBasedLocationResolver<>(blockPlacement);
         for (BasicBlock<V> blk : blocks) {
            blockPlacement.put(blk,pc);
            for (Instruction<V> ins : blk.instrs) {
               instrs[pc++] = ins.pack(resolver);
            }
         }

         return new RegexDfaVm<V>(instrs);
      }
   }
}

