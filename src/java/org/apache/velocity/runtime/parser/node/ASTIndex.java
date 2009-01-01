package org.apache.velocity.runtime.parser.node;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.runtime.parser.Parser;
import org.apache.velocity.runtime.parser.node.ASTMethod.MethodCacheKey;
import org.apache.velocity.util.ClassUtils;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.IntrospectionCacheData;
import org.apache.velocity.util.introspection.VelMethod;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

/**
 *  This node is responsible for the bracket notation at the end of
 *  a reference, e.g., $foo[1]
 */

public class ASTIndex extends SimpleNode
{
    private final String methodName = "get";

    /**
     * Indicates if we are running in strict reference mode.
     */
    protected boolean strictRef = false;

    public ASTIndex(int i)
    {
        super(i);
    }

    public ASTIndex(Parser p, int i)
    {
        super(p, i);
    }
  
    public Object init(InternalContextAdapter context, Object data)
        throws TemplateInitException
    {
        super.init(context, data);    
        strictRef = rsvc.getBoolean(RuntimeConstants.RUNTIME_REFERENCES_STRICT, false);
        return data;
    }  

    public Object execute(Object o, InternalContextAdapter context)
        throws MethodInvocationException
    {
        Object argument = jjtGetChild(0).value(context);
        Object [] params = {argument};
        Class[] paramClasses = {argument == null ? null : argument.getClass()};

        VelMethod method = ClassUtils.getMethod(methodName, params, paramClasses, 
                                                o, context, this, rsvc, strictRef);

        if (method == null) return null;
    
        try
        {
            /*
             *  get the returned object.  It may be null, and that is
             *  valid for something declared with a void return type.
             *  Since the caller is expecting something to be returned,
             *  as long as things are peachy, we can return an empty
             *  String so ASTReference() correctly figures out that
             *  all is well.
             */
            Object obj = method.invoke(o, params);

            if (obj == null)
            {
                if( method.getReturnType() == Void.TYPE)
                {
                    return "";
                }
            }

            return obj;
        }
        /**
         * pass through application level runtime exceptions
         */
        catch( RuntimeException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            String msg = "Error invoking method 'get("
              + (argument == null ? "null" : argument.getClass().getName()) 
              + ")' in " + o.getClass() 
              + " at " + Log.formatFileString(this);
            log.error(msg, e);
            throw new VelocityException(msg, e);
        }
    }  
}