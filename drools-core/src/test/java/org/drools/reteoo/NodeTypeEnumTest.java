package org.drools.reteoo;

import static org.junit.Assert.*;

import org.junit.Test;

import static org.drools.reteoo.NodeTypeEnums.*;

public class NodeTypeEnumTest {
    EntryPointNode         epNode     = new EntryPointNode();
    Rete                   reteNod    = new Rete();
    ObjectTypeNode         otNode     = new ObjectTypeNode();
    AlphaNode              alphaNode  = new AlphaNode();
    PropagationQueuingNode pqNpode    = new PropagationQueuingNode();
    WindowNode             winNode    = new WindowNode();

    RightInputAdapterNode  riaNode    = new RightInputAdapterNode();

    RuleTerminalNode       rtNode     = new RuleTerminalNode();
    QueryTerminalNode      qtNode     = new QueryTerminalNode();

    LeftInputAdapterNode   liaNode    = new LeftInputAdapterNode();

    EvalConditionNode      evalNode   = new EvalConditionNode();
    FromNode               fromNode   = new FromNode();

    QueryRiaFixerNode      qrfNode    = new QueryRiaFixerNode();
    QueryElementNode       uNode      = new QueryElementNode();

    NotNode                notNode    = new NotNode();
    ExistsNode             existsNode = new ExistsNode();
    JoinNode               joinNode   = new JoinNode();
    AccumulateNode         accNode    = new AccumulateNode();

    @Test
    public void tesObjectSource() {
        assertTrue( isObjectSource( epNode ) );
        assertTrue( isObjectSource( reteNod ) );
        assertTrue( isObjectSource( otNode ) );
        assertTrue( isObjectSource( alphaNode ) );
        assertTrue( isObjectSource( pqNpode ) );
        
        assertTrue( isObjectSource( riaNode ) );
        
        assertFalse( isObjectSource( rtNode ) );
        assertFalse( isObjectSource( qtNode ) );
        
        assertFalse( isObjectSource( liaNode ) );
        
        assertFalse( isObjectSource( evalNode ) );
        assertFalse( isObjectSource( fromNode ) );
        
        assertFalse( isObjectSource( qrfNode ) );
        assertFalse( isObjectSource( uNode ) );
        
        assertFalse( isObjectSource( notNode ) );
        assertFalse( isObjectSource( existsNode ) );
        assertFalse( isObjectSource( joinNode ) );
        assertFalse( isObjectSource( accNode ) );       
    }
    
    @Test
    public void tesObjectSink() {
        assertTrue( isObjectSink( epNode ) );
        assertTrue( isObjectSink( reteNod ) );
        assertTrue( isObjectSink( otNode ) );
        assertTrue( isObjectSink( alphaNode ) );
        assertTrue( isObjectSink( pqNpode ) );
        
        assertFalse( isObjectSink( riaNode ) );
        
        assertFalse( isObjectSink( rtNode ) );
        assertFalse( isObjectSink( qtNode ) );       
        
        assertTrue( isObjectSink( liaNode ) );
        
        assertFalse( isObjectSink( evalNode ) );
        assertFalse( isObjectSink( fromNode ) );
        
        assertFalse( isObjectSink( qrfNode ) );
        assertFalse( isObjectSink( uNode ) );
        
        assertFalse( isObjectSink( notNode ) );
        assertFalse( isObjectSink( existsNode ) );
        assertFalse( isObjectSink( joinNode ) );
        assertFalse( isObjectSink( accNode ) );       
    }    
    
    @Test
    public void tesLeftTupleSource() {
        assertFalse( isLeftTupleSource( epNode ) );
        assertFalse( isLeftTupleSource( reteNod ) );
        assertFalse( isLeftTupleSource( otNode ) );
        assertFalse( isLeftTupleSource( alphaNode ) );
        assertFalse( isLeftTupleSource( pqNpode ) );
        assertFalse( isLeftTupleSource( riaNode ) );
        
        assertFalse( isLeftTupleSource( rtNode ) );
        assertFalse( isLeftTupleSource( qtNode ) );
        
        assertTrue( isLeftTupleSource( liaNode ) );        
        
        assertTrue( isLeftTupleSource( evalNode ) );
        assertTrue( isLeftTupleSource( fromNode ) );
        
        assertTrue( isLeftTupleSource( qrfNode ) );
        assertTrue( isLeftTupleSource( uNode ) );
        
        assertTrue( isLeftTupleSource( notNode ) );
        assertTrue( isLeftTupleSource( existsNode ) );
        assertTrue( isLeftTupleSource( joinNode ) );
        assertTrue( isLeftTupleSource( accNode ) );       
    }    
    
    @Test
    public void tesLeftTupleSink() {
        assertFalse( isLeftTupleSink( epNode ) );
        assertFalse( isLeftTupleSink( reteNod ) );
        assertFalse( isLeftTupleSink( otNode ) );
        assertFalse( isLeftTupleSink( alphaNode ) );
        assertFalse( isLeftTupleSink( pqNpode ) );
        
        assertTrue( isLeftTupleSink( riaNode ) );
        
        assertTrue( isLeftTupleSink( rtNode ) );
        assertTrue( isLeftTupleSink( qtNode ) );
        
        assertFalse( isLeftTupleSink( liaNode ) );       
        
        assertTrue( isLeftTupleSink( evalNode ) );
        assertTrue( isLeftTupleSink( fromNode ) );
        
        assertTrue( isLeftTupleSink( qrfNode ) );
        assertTrue( isLeftTupleSink( uNode ) );
        
        assertTrue( isLeftTupleSink( notNode ) );
        assertTrue( isLeftTupleSink( existsNode ) );
        assertTrue( isLeftTupleSink( joinNode ) );
        assertTrue( isLeftTupleSink( accNode ) );       
    }     
    
    @Test
    public void testBetaNode() {
        assertFalse( isBetaNode( epNode ) );
        assertFalse( isBetaNode( reteNod ) );
        assertFalse( isBetaNode( otNode ) );
        assertFalse( isBetaNode( alphaNode ) );
        assertFalse( isBetaNode( pqNpode ) );
        
        assertFalse( isBetaNode( riaNode ) );
        
        assertFalse( isBetaNode( rtNode ) );
        assertFalse( isBetaNode( qtNode ) );
        
        assertFalse( isBetaNode( liaNode ) );       
        
        assertFalse( isBetaNode( evalNode ) );
        assertFalse( isBetaNode( fromNode ) );
        
        assertFalse( isBetaNode( qrfNode ) );
        assertFalse( isBetaNode( uNode ) );
        
        assertTrue( isBetaNode( notNode ) );
        assertTrue( isBetaNode( existsNode ) );
        assertTrue( isBetaNode( joinNode ) );
        assertTrue( isBetaNode( accNode ) );       
    }       
}
