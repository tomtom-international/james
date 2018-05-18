/*
 * Copyright 2017 TomTom International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tomtom.james.it.webservice;


import org.springframework.stereotype.Service;

@Service
public class TestService extends AbstractTestService implements IService {

    private final InternalClass ic = new InternalClass();

    public String methodOfSubclass(String arg0) {
        return "methodOfSubclass-value";
    }

    public String overloadedMethodOfSubclass(String arg0) {
        return "overloadedMethodOfSubclass-valueFor(String)";
    }

    public String overloadedMethodOfSubclass(int arg0) {
        return "overloadedMethodOfSubclass-valueFor(int)";
    }

    public String overloadedMethodOfSubclass(String arg0, int arg1) {
        return "overloadedMethodOfSubclass-valueFor(String,int)";
    }

    @Override
    String abstractMethodOfSuperclass(String arg0) {
        return "abstractMethodOfSuperclass-valueFromSubclass";
    }

    @Override
    String methodOfSuperclassOverriddenInSubclass(String arg0) {
        return "methodOfSuperclassOverriddenInSubclass-valueFromSubclass";
    }

    @Override
    String methodOfSuperclassOverriddenInSubclassCalledFromSubclass(String arg0) {
        String s = super.methodOfSuperclassOverriddenInSubclassCalledFromSubclass("arg-from-subclass");
        return "methodOfSuperclassOverriddenInSubclassCalledFromSubclass-valueFromSubclass";
    }

    @Override
    public String methodOfInterface(String arg0) {
        return "methodOfInterface-value";
    }

    public static String publicStaticMethod(String arg0) {
        return "publicStaticMethod-value";
    }

    public static String privateStaticMethodWrapper(String arg0) {
        return privateStaticMethod(arg0);
    }

    private static String privateStaticMethod(String arg0) {
        return "privateStaticMethod-value";
    }

    public InternalClass getInternalClass() {
        return ic;
    }

    @Override
    public String methodFromInterfaceOverridedByInterfaceImplementedInService(String arg0) {
        return "methodFromInterfaceOverridedByInterfaceImplementedInService-value";
    }

    public class InternalClass {
        public String methodOfInternalClass(String arg0) {
            return "methodOfInternalClass-value";
        }
    }
}
