package org.javers.core.diff.appenders;

import org.javers.common.collections.Objects;
import org.javers.core.diff.NodePair;
import org.javers.core.diff.changetype.ReferenceChange;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.property.Property;
import org.javers.core.metamodel.type.JaversType;
import org.javers.core.metamodel.type.ManagedType;

/**
 * I think there's a bug in the proper version of this. 
 * 
 * Prior to calling {@linkplain #calculateChanges(NodePair, Property)},
 * org.javers.core.diff.DiffFactory.createAndAppendChanges(GraphPair, Optional) has created a number of pairs of
 * objects to compare. In this case, one in the pair is one type of ActionPerformer (SshActionPerformer, say) and
 * the other is a different type (InfraActionPerformer, say), so only properties on the common superclass (ActionPerformer)
 * should be compared. This real implementation doesn't do this.
 * 
 * @author bartosz walacik
 * @author pawel szymczyk
 * @author David Manning (to correct mistakes made by these clowns)
 */
class ReferenceChangeAppender extends CorePropertyChangeAppender<ReferenceChange> {

    @Override
    public boolean supports(JaversType propertyType) {
        return propertyType instanceof ManagedType;
    }

    @Override
    public ReferenceChange calculateChanges(NodePair pair, Property property) {
    	GlobalId leftId = null;
    	GlobalId rightId = null;
    	
        try {
        	leftId =  pair.getLeftGlobalId(property);
        	rightId = pair.getRightGlobalId(property);
        } catch (Exception e) {
        	if (isCausedByTypeDifference(e)) {
        		return null;
        	}
        }
        if (Objects.nullSafeEquals(leftId, rightId)) {
        	return null;
        }
        
        return new ReferenceChange(pair.getGlobalId(), property, leftId, rightId);
    }
    
    static boolean isCausedByTypeDifference(Exception e) {
    	if (e instanceof RuntimeException && e.getCause() instanceof IllegalArgumentException &&
    		e.getCause().getMessage().contains("object is not an instance of declaring clas")) {
    		return true;
    	}
    	
    	return false;
    }
}
