/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.reteoo;

import org.drools.base.mvel.MVELEnabledExpression;
import org.drools.base.mvel.MVELSalienceExpression;
import static org.drools.reteoo.PropertySpecificUtil.calculateNegativeMask;
import static org.drools.reteoo.PropertySpecificUtil.calculatePositiveMask;
import static org.drools.reteoo.PropertySpecificUtil.getSettableProperties;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.drools.RuleBaseConfiguration;
import org.drools.base.ClassObjectType;
import org.drools.common.AgendaItem;
import org.drools.common.EventSupport;
import org.drools.common.InternalAgenda;
import org.drools.common.InternalFactHandle;
import org.drools.common.InternalWorkingMemory;
import org.drools.common.Memory;
import org.drools.common.MemoryFactory;
import org.drools.common.PropagationContextImpl;
import org.drools.common.ScheduledAgendaItem;
import org.drools.common.TruthMaintenanceSystemHelper;
import org.drools.common.UpdateContext;
import org.drools.reteoo.RuleRemovalContext.CleanupAdapter;
import org.drools.reteoo.builder.BuildContext;
import org.drools.rule.Declaration;
import org.drools.rule.GroupElement;
import org.drools.rule.Rule;
import org.drools.spi.Activation;
import org.drools.spi.PropagationContext;
import org.drools.time.impl.ExpressionIntervalTimer;
import org.kie.event.rule.MatchCancelledCause;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Leaf Rete-OO node responsible for enacting <code>Action</code> s on a
 * matched <code>Rule</code>.
 *
 * @see org.kie.rule.Rule
 */
public class RuleTerminalNode extends AbstractTerminalNode implements MemoryFactory {
    // ------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------

    private int                           sequence         = -1;  // -1 means not set

    private static final long             serialVersionUID = 510l;

    /** The rule to invoke upon match. */
    private Rule                          rule;
    
    /**
     * the subrule reference is needed to resolve declarations
     * because declarations may have different offsets in each subrule
     */
    private GroupElement                  subrule;
    private int                           subruleIndex;
    private Declaration[]                 declarations;
    
    private Declaration[]                 timerDelayDeclarations;
    private Declaration[]                 timerPeriodDeclarations;
    private Declaration[]                 salienceDeclarations;
    private Declaration[]                 enabledDeclarations;

    private LeftTupleSinkNode             previousTupleSinkNode;
    private LeftTupleSinkNode             nextTupleSinkNode;

    private boolean                       fireDirect;

    private int                           leftInputOtnId;

    private String                        consequenceName;

    private boolean                       unlinkingEnabled;

    // ------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------
    public RuleTerminalNode() {

    }

    /**
     *
     * @param id
     * @param source
     * @param rule
     * @param subrule
     * @param subruleIndex
     * @param context
     */
    public RuleTerminalNode(final int id,
                            final LeftTupleSource source,
                            final Rule rule,
                            final GroupElement subrule,
                            final int subruleIndex,
                            final BuildContext context) {
        super( id,
               context.getPartitionId(),
               context.getRuleBase().getConfiguration().isMultithreadEvaluation(),
               source );
        this.rule = rule;
        this.subrule = subrule;
        this.subruleIndex = subruleIndex;
        
        this.unlinkingEnabled = context.getRuleBase().getConfiguration().isUnlinkingEnabled();

        setFireDirect( rule.getActivationListener().equals( "direct" ) );

        setDeclarations( this.subrule.getOuterDeclarations() );

        initDeclaredMask(context);        
        initInferredMask();
    }
    
    public void setDeclarations(Map<String, Declaration> decls) {
        if ( rule.getSalience() instanceof MVELSalienceExpression ) {
            MVELSalienceExpression expr = ( MVELSalienceExpression ) rule.getSalience();
            Declaration[] declrs = expr.getMVELCompilationUnit().getPreviousDeclarations();
            
            this.salienceDeclarations = new Declaration[declrs.length];
            int i = 0;
            for ( Declaration declr : declrs ) {
                this.salienceDeclarations[i++] = decls.get( declr.getIdentifier() );
            }
            Arrays.sort( this.salienceDeclarations, SortDeclarations.instance );            
        }
        
        if ( rule.getEnabled() instanceof MVELEnabledExpression ) {
            MVELEnabledExpression expr = ( MVELEnabledExpression ) rule.getEnabled();
            Declaration[] declrs = expr.getMVELCompilationUnit().getPreviousDeclarations();
            
            this.enabledDeclarations = new Declaration[declrs.length];
            int i = 0;
            for ( Declaration declr : declrs ) {
                this.enabledDeclarations[i++] = decls.get( declr.getIdentifier() );
            }
            Arrays.sort( this.enabledDeclarations, SortDeclarations.instance );              
        }        
        
        if ( rule.getTimer() instanceof ExpressionIntervalTimer ) {
            ExpressionIntervalTimer expr = ( ExpressionIntervalTimer ) rule.getTimer();
            
            Declaration[] declrs = expr.getDelayMVELCompilationUnit().getPreviousDeclarations();            
            this.timerDelayDeclarations = new Declaration[declrs.length];
            int i = 0;
            for ( Declaration declr : declrs ) {
                this.timerDelayDeclarations[i++] = decls.get( declr.getIdentifier() );
            }
            Arrays.sort( this.timerDelayDeclarations, SortDeclarations.instance );      
            
            declrs = expr.getPeriodMVELCompilationUnit().getPreviousDeclarations();            
            this.timerPeriodDeclarations = new Declaration[declrs.length];
            i = 0;
            for ( Declaration declr : declrs ) {
                this.timerPeriodDeclarations[i++] = decls.get( declr.getIdentifier() );
            }
            Arrays.sort( this.timerPeriodDeclarations, SortDeclarations.instance );             
        }
    }
    
    // ------------------------------------------------------------
    // Instance methods
    // ------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal(in);
        sequence = in.readInt();
        rule = (Rule) in.readObject();
        subrule = (GroupElement) in.readObject();
        subruleIndex = in.readInt();
        previousTupleSinkNode = (LeftTupleSinkNode) in.readObject();
        nextTupleSinkNode = (LeftTupleSinkNode) in.readObject();
        declarations = ( Declaration[]) in.readObject();

        timerDelayDeclarations = ( Declaration[]) in.readObject();
        timerPeriodDeclarations = ( Declaration[]) in.readObject();
        salienceDeclarations = ( Declaration[]) in.readObject();
        enabledDeclarations = ( Declaration[]) in.readObject();
        consequenceName = (String) in.readObject();

        fireDirect = rule.getActivationListener().equals( "direct" );

        leftInputOtnId = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        out.writeInt( sequence );
        out.writeObject( rule );
        out.writeObject( subrule );
        out.writeInt( subruleIndex );
        out.writeObject( previousTupleSinkNode );
        out.writeObject( nextTupleSinkNode );
        out.writeObject( declarations );
        
        out.writeObject( timerDelayDeclarations );
        out.writeObject( timerPeriodDeclarations );
        out.writeObject( salienceDeclarations );
        out.writeObject( enabledDeclarations );
        out.writeObject( consequenceName );

        out.writeLong(leftInputOtnId);
    }

    /**
     * Retrieve the <code>Action</code> associated with this node.
     *
     * @return The <code>Action</code> associated with this node.
     */
    public Rule getRule() {
        return this.rule;
    }

    public GroupElement getSubRule() {
        return this.subrule;
    }

    public void setSequence(int seq) {
        this.sequence = seq;
    }

    public int getSequence() {
        return this.sequence;
    }

    public void assertLeftTuple(final LeftTuple leftTuple,
                                PropagationContext context,
                                final InternalWorkingMemory workingMemory) {
        //check if the rule is not effective or
        // if the current Rule is no-loop and the origin rule is the same then return
        if ( (!this.rule.isEffective( leftTuple,
                                      this,
                                      workingMemory )) ||
             (this.rule.isNoLoop() && this.rule.equals( context.getRuleOrigin() )) ) {
            leftTuple.setObject( Boolean.TRUE );
            return;
        }

        final InternalAgenda agenda = (InternalAgenda) workingMemory.getAgenda();
        
        if( unlinkingEnabled ) {
            // Find the most recent PropagationContext, as this caused this rule to elegible for firing
            LeftTuple lt = leftTuple;
            while ( lt != null ) {
                if ( lt.getPropagationContext() != null && lt.getPropagationContext().getPropagationNumber() > context.getPropagationNumber() ) {
                    context = lt.getPropagationContext();
                }
                lt = lt.getParent();
            }
        }

        boolean fire = agenda.createActivation( leftTuple, 
                                                context, 
                                                workingMemory, 
                                                this, 
                                                false );
        if( fire && !fireDirect ) {
            agenda.addActivation( (AgendaItem) leftTuple.getObject() );
        }
    }

    public void modifyLeftTuple(LeftTuple leftTuple,
                                PropagationContext context,
                                InternalWorkingMemory workingMemory) {
    	InternalAgenda agenda = (InternalAgenda) workingMemory.getAgenda();
    	
        // we need the inserted facthandle so we can update the network with new Activation
    	Object o = leftTuple.getObject();
    	if ( o != Boolean.TRUE) {  // would be true due to lock-on-active blocking activation creation
    		AgendaItem match = (AgendaItem) o;       
	        if ( match != null && match.isActivated() ) {
	            // already activated, do nothing
	            // although we need to notify the inserted Activation, as it's declarations may have changed.
	            agenda.modifyActivation( match, true );
	            return;
	        }
    	}

        // if the current Rule is no-loop and the origin rule is the same then return
        if ( (!this.rule.isEffective( leftTuple,
                                      this,
                                      workingMemory )) ||
             (this.rule.isNoLoop() && this.rule.equals( context.getRuleOrigin() )) ) {
            agenda.increaseDormantActivations();
            return;
        }

        boolean reuseActivation = true;
        if ( o  == Boolean.TRUE ) {
        	// set to Boolean.TRUE when lock-on-active stops an Activation being created
        	// We set this instead of doing a null check, as it's a little safer due to intent.
        	reuseActivation = false;
        	leftTuple.setObject( null );
        }
        boolean fire = agenda.createActivation( leftTuple, context, workingMemory, this, reuseActivation );
        if ( fire && !isFireDirect() ) {
            // This activation is currently dormant and about to reactivated, so decrease the dormant count.
            agenda.decreaseDormantActivations();

            agenda.modifyActivation( (AgendaItem) leftTuple.getObject(), false );
        }
    }

    public void retractLeftTuple(final LeftTuple leftTuple,
                                 final PropagationContext context,
                                 final InternalWorkingMemory workingMemory) {
        Object obj = leftTuple.getObject();


        // activation can be null if the LeftTuple previous propagated into a no-loop
        // or could be true due to lock-on-active blocking activation creation
        if ( obj == null || obj == Boolean.TRUE) {
            return;
        }

        Activation activation = (Activation) obj;
        activation.setMatched( false );
        
        InternalAgenda agenda = (InternalAgenda) workingMemory.getAgenda();

        agenda.cancelActivation( leftTuple, 
                                 context, 
                                 workingMemory, 
                                 activation, 
                                 this );
    }


    public String toString() {
        return "[RuleTerminalNode(" + this.getId() + "): rule=" + this.rule.getName() + "]";
    }

    public void attach( BuildContext context ) {
        getLeftTupleSource().addTupleSink(this, context);
        if (context == null) {
            return;
        }

        for ( InternalWorkingMemory workingMemory : context.getWorkingMemories() ) {
            final PropagationContext propagationContext = new PropagationContextImpl( workingMemory.getNextPropagationIdCounter(),
                                                                                      PropagationContext.RULE_ADDITION,
                                                                                      null,
                                                                                      null,
                                                                                      null );
            getLeftTupleSource().updateSink(this, propagationContext, workingMemory);
        }
    }

    public void networkUpdated(UpdateContext updateContext) {
        getLeftTupleSource().networkUpdated(updateContext);
    }

    protected void doRemove(final RuleRemovalContext context,
                            final ReteooBuilder builder,
                            final InternalWorkingMemory[] workingMemories) {
        getLeftTupleSource().removeTupleSink(this);
    }

    protected void doCollectAncestors(NodeSet nodeSet) {
        getLeftTupleSource().collectAncestors(nodeSet);
    }

    public boolean isInUse() {
        return false;
    }

    public boolean isLeftTupleMemoryEnabled() {
        return false;
    }

    public void setLeftTupleMemoryEnabled(boolean tupleMemoryEnabled) {

    }

    public Declaration[] getDeclarations() {
        if ( this.declarations == null ) {
            Map<String, Declaration> decls = this.subrule.getOuterDeclarations();
            String[] requiredDeclarations = rule.getRequiredDeclarationsForConsequence(getConsequenceName());
            this.declarations = new Declaration[requiredDeclarations.length];
            int i = 0;
            for ( String str : requiredDeclarations ) {
                declarations[i++] = decls.get( str );
            }
            Arrays.sort( this.declarations, SortDeclarations.instance );
        }
        return this.declarations;
    }
    
    public Declaration[] getTimerDelayDeclarations() {
        return timerDelayDeclarations;
    }

    public void setTimerDelayDeclarations(Declaration[] timerDelayDeclarations) {
        this.timerDelayDeclarations = timerDelayDeclarations;
    }

    public Declaration[] getTimerPeriodDeclarations() {
        return timerPeriodDeclarations;
    }

    public void setTimerPeriodDeclarations(Declaration[] timerPeriodDeclarations) {
        this.timerPeriodDeclarations = timerPeriodDeclarations;
    }

    public Declaration[] getSalienceDeclarations() {
        return salienceDeclarations;
    }

    public void setSalienceDeclarations(Declaration[] salienceDeclarations) {
        this.salienceDeclarations = salienceDeclarations;
    }

    public Declaration[] getEnabledDeclarations() {
        return enabledDeclarations;
    }

    public void setEnabledDeclarations(Declaration[] enabledDeclarations) {
        this.enabledDeclarations = enabledDeclarations;
    }

    public String getConsequenceName() {
        return consequenceName == null ? Rule.DEFAULT_CONSEQUENCE_NAME : consequenceName;
    }

    public void setConsequenceName(String consequenceName) {
        this.consequenceName = consequenceName;
    }


    public static class SortDeclarations
            implements
            Comparator<Declaration> {
        public final static SortDeclarations instance = new SortDeclarations();

        public int compare(Declaration d1,
                           Declaration d2) {
            return (d1.getIdentifier().compareTo( d2.getIdentifier() ));
        }
    }

    /**
     * Returns the next node
     * @return
     *      The next TupleSinkNode
     */
    public LeftTupleSinkNode getNextLeftTupleSinkNode() {
        return this.nextTupleSinkNode;
    }

    /**
     * Sets the next node
     * @param next
     *      The next TupleSinkNode
     */
    public void setNextLeftTupleSinkNode(final LeftTupleSinkNode next) {
        this.nextTupleSinkNode = next;
    }

    /**
     * Returns the previous node
     * @return
     *      The previous TupleSinkNode
     */
    public LeftTupleSinkNode getPreviousLeftTupleSinkNode() {
        return this.previousTupleSinkNode;
    }

    /**
     * Sets the previous node
     * @param previous
     *      The previous TupleSinkNode
     */
    public void setPreviousLeftTupleSinkNode(final LeftTupleSinkNode previous) {
        this.previousTupleSinkNode = previous;
    }

    public int hashCode() {
        return this.rule.hashCode();
    }

    public boolean equals(final Object object) {
        if ( object == this ) {
            return true;
        }

        if ( !(object instanceof RuleTerminalNode) ) {
            return false;
        }

        final RuleTerminalNode other = (RuleTerminalNode) object;
        return rule.equals(other.rule) && (consequenceName == null ? other.consequenceName == null : consequenceName.equals(other.consequenceName));
    }

    public short getType() {
        return NodeTypeEnums.RuleTerminalNode;
    }

    public static class RTNCleanupAdapter
            implements
            CleanupAdapter {
        private final RuleTerminalNode node;

        public RTNCleanupAdapter(RuleTerminalNode node) {
            this.node = node;
        }

        public void cleanUp(final LeftTuple leftTuple,
                            final InternalWorkingMemory workingMemory) {
            if ( leftTuple.getLeftTupleSink() != node ) {
                return;
            }

            final Activation activation = (Activation) leftTuple.getObject();

            // this is to catch a race condition as activations are activated and unactivated on timers
            if ( activation instanceof ScheduledAgendaItem ) {
                ScheduledAgendaItem scheduled = (ScheduledAgendaItem) activation;
                workingMemory.getTimerService().removeJob( scheduled.getJobHandle() );
                scheduled.getJobHandle().setCancel( true );
            }

            if ( activation.isActivated() ) {
                activation.remove();
                ((EventSupport) workingMemory).getAgendaEventSupport().fireActivationCancelled( activation,
                                                                                                workingMemory,
                                                                                                MatchCancelledCause.CLEAR );
            }

            final PropagationContext propagationContext = new PropagationContextImpl( workingMemory.getNextPropagationIdCounter(),
                                                                                      PropagationContext.RULE_REMOVAL,
                                                                                      null,
                                                                                      null,
                                                                                      null );
            TruthMaintenanceSystemHelper.removeLogicalDependencies( activation,
                                                                    propagationContext,
                                                                    node.getRule() );
            leftTuple.unlinkFromLeftParent();
            leftTuple.unlinkFromRightParent();
        }
    }

    public LeftTuple createLeftTuple(InternalFactHandle factHandle,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new RuleTerminalNodeLeftTuple( factHandle, sink, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new RuleTerminalNodeLeftTuple( leftTuple, sink, leftTupleMemoryEnabled );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTupleSink sink) {
        return new RuleTerminalNodeLeftTuple( leftTuple, rightTuple, sink );
    }

    public LeftTuple createLeftTuple(LeftTuple leftTuple,
                                     RightTuple rightTuple,
                                     LeftTuple currentLeftChild,
                                     LeftTuple currentRightChild,
                                     LeftTupleSink sink,
                                     boolean leftTupleMemoryEnabled) {
        return new RuleTerminalNodeLeftTuple(leftTuple, rightTuple, currentLeftChild, currentRightChild, sink, leftTupleMemoryEnabled );        

    }      
    
    public int getLeftInputOtnId() {
        return leftInputOtnId;
    }

    public void setLeftInputOtnId(int leftInputOtnId) {
        this.leftInputOtnId = leftInputOtnId;
    }  

    public boolean isFireDirect() {
        return fireDirect;
    }

    public void setFireDirect(boolean fireDirect) {
        this.fireDirect = fireDirect;
    }

    protected ObjectTypeNode getObjectTypeNode() {
        return getLeftTupleSource().getObjectTypeNode();
    }

    public Memory createMemory(RuleBaseConfiguration config) {
        int segmentCount = 1; // always atleast one segment
        int segmentPosMask = 1;
        long allLinkedTestMask = 1;        
        RuleMemory rmem = new RuleMemory(this);
        LeftTupleSource tupleSource = getLeftTupleSource();
        boolean updateBitInNewSegment = false; // this is so we can handle segments that don't have betanode's, as their bit will never be set
        while ( tupleSource.getLeftTupleSource() != null ) {            
            if ( !BetaNode.parentInSameSegment( tupleSource ) ) {
                updateBitInNewSegment = true;
                segmentPosMask = segmentPosMask << 1;  
                segmentCount++;
            }
            
            if ( updateBitInNewSegment && NodeTypeEnums.isBetaNode( tupleSource )) {
                updateBitInNewSegment = false;
                allLinkedTestMask = allLinkedTestMask | segmentPosMask;
            }
            
            tupleSource = tupleSource.getLeftTupleSource();            
        }        
        rmem.setAllLinkedMaskTest( allLinkedTestMask );
        rmem.setSegmentMemories( new SegmentMemory[segmentCount] );
        return rmem;
    }

    public LeftTuple createPeer(LeftTuple original) {
        throw new UnsupportedOperationException();
    }            

}
