package org.javers.core.diff.appenders;

import static org.javers.core.diff.appenders.ReferenceChangeAppender.isCausedByTypeDifference;

import org.javers.common.collections.Objects;
import org.javers.core.diff.NodePair;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.property.Property;
import org.javers.core.metamodel.type.JaversType;
import org.javers.core.metamodel.type.PrimitiveOrValueType;

/**
 * @author bartosz walacik
 */
class ValueChangeAppender extends CorePropertyChangeAppender<ValueChange> {

    @Override
    public boolean supports(JaversType propertyType) {
        return  propertyType instanceof PrimitiveOrValueType;
    }

    /**
     * @param property supported property (of PrimitiveType or ValueObjectType)
     */
    @Override
    public ValueChange calculateChanges(NodePair pair, Property property) {
    	Object leftValue = null;
        Object rightValue = null;
    	try {
	        leftValue = pair.getLeftPropertyValue(property);
	        rightValue = pair.getRightPropertyValue(property);
    	} catch (Exception e) {
    		if (isCausedByTypeDifference(e)) {
    			return null;
    		}
    	}

        if (Objects.nullSafeEquals(leftValue,rightValue)) {
            return null;
        }

        return new ValueChange(pair.getGlobalId(), property, leftValue, rightValue);
    }
}
