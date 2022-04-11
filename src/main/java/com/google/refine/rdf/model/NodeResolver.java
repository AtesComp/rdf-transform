package com.google.refine.rdf.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class NodeResolver extends TypeIdResolverBase  {
    protected TypeFactory factory = TypeFactory.defaultInstance();

    private static Map<String, Class<? extends Node>> registry =
        new HashMap<String, Class<? extends Node>>() {{
            put( ConstantLiteralNode.getNODETYPE(),     ConstantLiteralNode.class  );
            put( ConstantBlankNode.getNODETYPE(),       ConstantBlankNode.class    );
            put( ConstantResourceNode.getNODETYPE(),    ConstantResourceNode.class );
            put( CellLiteralNode.getNODETYPE(),         CellLiteralNode.class      );
            put( CellBlankNode.getNODETYPE(),           CellBlankNode.class        );
            put( CellResourceNode.getNODETYPE(),        CellResourceNode.class     );
        }};
    

    @Override
    public Id getMechanism() {
        return Id.NAME;
    }

    @Override
    public String idFromValue(Object instance) {
        return ( (Node) instance ).getNodeType();
    }

    @Override
    public String idFromValueAndType(Object instance, Class<?> type) {
        if (instance == null && type != null) {
            if ( type == ConstantLiteralNode.class ) {
                return ConstantLiteralNode.getNODETYPE();
            }
            if ( type == ConstantBlankNode.class ) {
                return ConstantBlankNode.getNODETYPE();
            }
            if ( type == ConstantResourceNode.class ) {
                return ConstantResourceNode.getNODETYPE();
            }
            if ( type == CellLiteralNode.class ) {
                return CellLiteralNode.getNODETYPE();
            }
            if ( type == CellBlankNode.class ) {
                return CellBlankNode.getNODETYPE();
            }
            if ( type == CellResourceNode.class ) {
                return CellResourceNode.getNODETYPE();
            }
        }
        return ( (Node) instance ).getNodeType();
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String strID) throws IOException {
        return factory.constructSimpleType(registry.get(strID), new JavaType[0]);
    }

    /**
     * Helper method used to get a simple description of all known type ids,
     * for use in error messages.
     */
    @Override
    public String getDescForKnownTypeIds() {
        return "Node Classes";
    }
}
