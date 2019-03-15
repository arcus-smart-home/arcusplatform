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
package com.iris.model.query.expression;

import static java.lang.Boolean.parseBoolean;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.iris.messages.model.Model;
import com.iris.model.query.antlr.ModelExpressionBaseListener;
import com.iris.model.query.antlr.ModelExpressionLexer;
import com.iris.model.query.antlr.ModelExpressionParser;
import com.iris.model.query.antlr.ModelExpressionParser.AttributeContext;
import com.iris.model.query.antlr.ModelExpressionParser.AttributePredicateContext;
import com.iris.model.query.antlr.ModelExpressionParser.BinaryPredicateContext;
import com.iris.model.query.antlr.ModelExpressionParser.ConstantPredicateContext;
import com.iris.model.query.antlr.ModelExpressionParser.GroupPredicateContext;
import com.iris.model.query.antlr.ModelExpressionParser.NamespaceContext;
import com.iris.model.query.antlr.ModelExpressionParser.NamespacePredicateContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorAndContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorContainsContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorGreaterThanContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorGreaterThanOrEqualToContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorHasAContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorIsAContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorIsSupportedContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorLessThanContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorLessThanOrEqualToContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorLikeContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorNotEqualsContext;
import com.iris.model.query.antlr.ModelExpressionParser.OperatorOrContext;
import com.iris.model.query.antlr.ModelExpressionParser.UnaryPredicateContext;
import com.iris.model.query.antlr.ModelExpressionParser.ValueBooleanContext;
import com.iris.model.query.antlr.ModelExpressionParser.ValueNumericContext;
import com.iris.model.query.antlr.ModelExpressionParser.ValueStringContext;
import com.iris.model.query.expression.ExpressionBuilder.ComparisonExpressionBuilder;
import com.iris.model.query.expression.ExpressionBuilder.ConstantComparisonBuilder;
import com.iris.model.query.expression.ExpressionBuilder.AttributeComparisonBuilder;
import com.iris.model.query.expression.ExpressionBuilder.BinaryExpressionBuilder;
import com.iris.model.query.expression.ExpressionBuilder.NamespaceExpressionBuilder;
import com.iris.model.query.expression.ExpressionBuilder.NestedExpressionBuilder;

public class ExpressionCompiler {
   private static final Logger logger = LoggerFactory.getLogger(ExpressionCompiler.class);

   public static Predicate<Model> compile(String expression) {
      try {
         return compile(new ByteArrayInputStream(expression.getBytes()));
      }
      catch (IOException e) {
         // should never happen from a string
         throw new RuntimeException(e);
      }
   }

   public static Predicate<Model> compile(InputStream is) throws IOException {
      return compile(new ANTLRInputStream(is));
   }

   public static Predicate<Model> compile(File file) throws IOException {
      return compile(new ANTLRFileStream(file.getAbsolutePath()));
   }

   private static Predicate<Model> compile(ANTLRInputStream ais) {
      ModelExpressionLexer mel = new ModelExpressionLexer(ais);
      CommonTokenStream stream = new CommonTokenStream(mel);
      ModelExpressionParser parser = new ModelExpressionParser(stream);
      ModelExpressionListener listener = new ModelExpressionListener(parser);
      
      ParseTreeWalker chuck = new ParseTreeWalker();
      chuck.walk(listener, parser.expression());
      return listener.build();
   }
   
   private static class ModelExpressionListener extends ModelExpressionBaseListener {
      private ModelExpressionParser parser;
      private ExpressionBuilder builder = ExpressionBuilder.create();
      private StringBuilder errors = new StringBuilder();
      
      ModelExpressionListener(ModelExpressionParser parser) {
         this.parser = parser;
      }
      
      public Predicate<Model> build() {
         if(errors.length() > 0) {
            throw new IllegalArgumentException("Invalid expression:" + errors.toString());
         }
         return builder.build(); 
      }
      
      @Override
      public void enterAttributePredicate(AttributePredicateContext ctx) {
         builder = ((NestedExpressionBuilder) builder).openAttributeExpression();
      }

      @Override
      public void exitAttributePredicate(AttributePredicateContext ctx) {
         builder = ((AttributeComparisonBuilder) builder).closeExpression();
      }

      @Override
      public void enterConstantPredicate(ConstantPredicateContext ctx) {
         builder = ((NestedExpressionBuilder) builder).openConstantExpression();
      }

      @Override
      public void exitConstantPredicate(ConstantPredicateContext ctx) {
         builder = ((ConstantComparisonBuilder) builder).closeExpression();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterAttribute(com.iris.model.query.antlr.ModelExpressionParser.AttributeContext)
       */
      @Override
      public void enterAttribute(AttributeContext ctx) {
         ((ComparisonExpressionBuilder) builder).withAttributeName(ctx.getText());
      }

      @Override
		public void enterNamespacePredicate(NamespacePredicateContext ctx) {
			builder = ((NestedExpressionBuilder) builder).openNamespaceExpression();
		}

		@Override
		public void exitNamespacePredicate(NamespacePredicateContext ctx) {
			builder = ((NamespaceExpressionBuilder) builder).closeExpression();
		}

		@Override
		public void enterNamespace(NamespaceContext ctx) {
			((NamespaceExpressionBuilder) builder).withNamespace(ctx.getText());
		}

		@Override
		public void enterOperatorIsA(OperatorIsAContext ctx) {
			((NamespaceExpressionBuilder) builder).isA();
		}

		@Override
		public void enterOperatorHasA(OperatorHasAContext ctx) {
			((NamespaceExpressionBuilder) builder).hasA();
		}

		/* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterValueBoolean(com.iris.model.query.antlr.ModelExpressionParser.ValueBooleanContext)
       */
      @Override
      public void enterValueBoolean(ValueBooleanContext ctx) {
         ((ComparisonExpressionBuilder) builder).withValue(parseBoolean(ctx.getText()));
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterValueNumeric(com.iris.model.query.antlr.ModelExpressionParser.ValueNumericContext)
       */
      @Override
      public void enterValueNumeric(ValueNumericContext ctx) {
         ((ComparisonExpressionBuilder) builder).withValue(Double.parseDouble(ctx.getText()));
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterValueString(com.iris.model.query.antlr.ModelExpressionParser.ValueStringContext)
       */
      @Override
      public void enterValueString(ValueStringContext ctx) {
         String text = ctx.getText();
         // Remove quotes
         String value = text.substring(1, text.length() - 1);
         ((ComparisonExpressionBuilder) builder).withValue(value);
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorEquals(com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext)
       */
      @Override
      public void enterOperatorEquals(OperatorEqualsContext ctx) {
         ((ComparisonExpressionBuilder) builder).equals();
      }
      
      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorEquals(com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext)
       */
      @Override
      public void enterOperatorNotEquals(OperatorNotEqualsContext ctx) {
         ((ComparisonExpressionBuilder) builder).notEquals();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorLike(com.iris.model.query.antlr.ModelExpressionParser.OperatorLikeContext)
       */
      @Override
      public void enterOperatorLike(OperatorLikeContext ctx) {
         ((ComparisonExpressionBuilder) builder).like();
      }

      @Override
      public void enterOperatorContains(OperatorContainsContext ctx) {
         ((ComparisonExpressionBuilder) builder).contains();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorEquals(com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext)
       */
      @Override
      public void enterOperatorGreaterThan(OperatorGreaterThanContext ctx) {
         ((ComparisonExpressionBuilder) builder).greaterThan();
      }

     
      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorEquals(com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext)
       */
      @Override
      public void enterOperatorGreaterThanOrEqualTo(OperatorGreaterThanOrEqualToContext ctx) {
         ((ComparisonExpressionBuilder) builder).greaterThanEqualTo();
      }
      
      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorEquals(com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext)
       */
      @Override
      public void enterOperatorLessThan(OperatorLessThanContext ctx) {
         ((ComparisonExpressionBuilder) builder).lessThan();
      }

     
      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorEquals(com.iris.model.query.antlr.ModelExpressionParser.OperatorEqualsContext)
       */
      @Override
      public void enterOperatorLessThanOrEqualTo(OperatorLessThanOrEqualToContext ctx) {
         ((ComparisonExpressionBuilder) builder).lessThanEqualTo();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorIsSupported(com.iris.model.query.antlr.ModelExpressionParser.OperatorIsSupportedContext)
       */
      @Override
      public void enterOperatorIsSupported(OperatorIsSupportedContext ctx) {
         ((ComparisonExpressionBuilder) builder).isSupported();
      }

		/* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterBinaryPredicate(com.iris.model.query.antlr.ModelExpressionParser.BinaryPredicateContext)
       */
      @Override
      public void enterBinaryPredicate(BinaryPredicateContext ctx) {
         builder = ((NestedExpressionBuilder) builder).openBinaryExpression();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#exitBinaryPredicate(com.iris.model.query.antlr.ModelExpressionParser.BinaryPredicateContext)
       */
      @Override
      public void exitBinaryPredicate(BinaryPredicateContext ctx) {
         builder = builder.closeExpression();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorAnd(com.iris.model.query.antlr.ModelExpressionParser.OperatorAndContext)
       */
      @Override
      public void enterOperatorAnd(OperatorAndContext ctx) {
      	builder = ((BinaryExpressionBuilder) builder).and();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterOperatorOr(com.iris.model.query.antlr.ModelExpressionParser.OperatorOrContext)
       */
      @Override
      public void enterOperatorOr(OperatorOrContext ctx) {
         builder = ((BinaryExpressionBuilder) builder).or();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterGroupPredicate(com.iris.model.query.antlr.ModelExpressionParser.GroupPredicateContext)
       */
      @Override
      public void enterGroupPredicate(GroupPredicateContext ctx) {
         builder = ((NestedExpressionBuilder) builder).openExpression();
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#exitGroupPredicate(com.iris.model.query.antlr.ModelExpressionParser.GroupPredicateContext)
       */
      @Override
      public void exitGroupPredicate(GroupPredicateContext ctx) {
         builder = ((NestedExpressionBuilder) builder).closeExpression();
      }

      @Override
		public void enterUnaryPredicate(UnaryPredicateContext ctx) {
      	// TODO NOT is currently the only unary operator, so we cheat a bit here by opening it now
      	//      technically should be openUnaryExpression() then set operator to not
         builder = ((NestedExpressionBuilder) builder).openNotExpression();
		}

		@Override
		public void exitUnaryPredicate(UnaryPredicateContext ctx) {
			builder = builder.closeExpression();
		}

		/* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#enterEveryRule(org.antlr.v4.runtime.ParserRuleContext)
       */
      @Override
      public void enterEveryRule(ParserRuleContext ctx) {
         if(logger.isTraceEnabled()) {
            logger.trace("Context: {}", builder);
            logger.trace(">>Enter {}: {}", parser.getRuleNames()[ctx.getRuleIndex()], ctx.getText());
         }
      }

      @Override
      public void exitEveryRule(ParserRuleContext ctx) {
         if(logger.isTraceEnabled()) {
            logger.trace("<<Exit {}", parser.getRuleNames()[ctx.getRuleIndex()]);
            logger.trace("Context: {}", builder);
         }
      }

      /* (non-Javadoc)
       * @see com.iris.model.query.antlr.ModelExpressionBaseListener#visitErrorNode(org.antlr.v4.runtime.tree.ErrorNode)
       */
      @Override
      public void visitErrorNode(ErrorNode node) {
         logger.debug("Error parsing expression {}", node);
         errors.append("\n\t").append(node);
      }
      
   }
}

