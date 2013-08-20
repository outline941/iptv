/*
 *
 * Copyright  1990-2008 Sun Microsystems, Inc. All Rights Reserved.  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER  
 *   
 * This program is free software; you can redistribute it and/or  
 * modify it under the terms of the GNU General Public License version  
 * 2 only, as published by the Free Software Foundation.   
 *   
 * This program is distributed in the hope that it will be useful, but  
 * WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
 * General Public License version 2 for more details (a copy is  
 * included at /legal/license.txt).   
 *   
 * You should have received a copy of the GNU General Public License  
 * version 2 along with this work; if not, write to the Free Software  
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  
 * 02110-1301 USA   
 *   
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa  
 * Clara, CA 95054 or visit www.sun.com if you need additional  
 * information or have any questions. 
 *
 */
#include "jni.h"
#include "jvm.h"
#include "javavm/include/classes.h"
#include "javavm/include/interpreter.h"

/*
 * This is the linked-list representation of class members
 * that the filter allows.
 */
struct linkedClassRestriction {
    /* next element in the list */
    struct linkedClassRestriction*	next;
    /* typeid of this class */ 
    CVMClassTypeID		 	thisClass;
    /* number of methods in the class */
    int					nMethods;
    /* number of fields in the class */ 
    int					nFields;
    /* methods of the class */
    CVMMethodTypeID*			methods;
    /* fields of the class */
    CVMFieldTypeID*			fields;
};

/*
 * This is the array-element representation of class
 * members that the filter allows.
 */
typedef struct CVMClassRestrictionElement {
    /* typeid of the class */
    CVMClassTypeID		 	thisClass;
    /* number of methods in the class */
    int					nMethods;
    /* number of fields in the class */
    int					nFields;
    /* methods of the class */
    CVMMethodTypeID*			methods;
    /* fields of the class */
    CVMFieldTypeID*			fields;
} CVMClassRestrictionElement;


struct CVMClassRestrictions{
    /* number of elements */
    int					nElements;
    struct CVMClassRestrictionElement*  restriction;
};

/* This is the filter data generated by ROMizer */
extern const struct CVMClassRestrictions CVMdualStackMemberFilter;

/* Check if the super MB exists */
extern CVMBool
CVMdualStackFindSuperMB(CVMExecEnv* ee,
                        CVMClassBlock* currentCB,
                        CVMClassBlock* superCB,
                        CVMMethodBlock* superMB);

/* 
 * Check if the classloader is one of the MIDP dual-stack classloaders.
 */
CVMBool
CVMclassloaderIsMIDPClassLoader(CVMExecEnv *ee,
                                CVMClassLoaderICell* loaderICell,
                                CVMBool checkImplClassLoader);