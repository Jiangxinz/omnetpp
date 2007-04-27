package org.omnetpp.ned.model.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.pojo.ChannelInterfaceNode;
import org.omnetpp.ned.model.pojo.ChannelNode;
import org.omnetpp.ned.model.pojo.ChannelSpecNode;
import org.omnetpp.ned.model.pojo.CompoundModuleNode;
import org.omnetpp.ned.model.pojo.ConnectionGroupNode;
import org.omnetpp.ned.model.pojo.ConnectionNode;
import org.omnetpp.ned.model.pojo.ConnectionsNode;
import org.omnetpp.ned.model.pojo.GateNode;
import org.omnetpp.ned.model.pojo.GatesNode;
import org.omnetpp.ned.model.pojo.ImportNode;
import org.omnetpp.ned.model.pojo.ModuleInterfaceNode;
import org.omnetpp.ned.model.pojo.NedFileNode;
import org.omnetpp.ned.model.pojo.ParamNode;
import org.omnetpp.ned.model.pojo.ParametersNode;
import org.omnetpp.ned.model.pojo.PropertyNode;
import org.omnetpp.ned.model.pojo.SimpleModuleNode;
import org.omnetpp.ned.model.pojo.SubmoduleNode;
import org.omnetpp.ned.model.pojo.SubmodulesNode;
import org.omnetpp.ned.model.pojo.TypesNode;

/**
 * A content provider that gives a basic overview structure for a ned model tree
 * @author rhornig
 */
public class NedModelContentProvider implements ITreeContentProvider {

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}

	public Object getParent(Object child) {
		return ((INEDElement)child).getParent();
	}

	public Object[] getChildren(Object parent) {
        List<INEDElement> result = new ArrayList<INEDElement>();
        INEDElement currElem = ((INEDElement)parent);

        // if this is a channel spec we will give back the parameters subnode's children
        if (currElem instanceof ChannelSpecNode)
            currElem = ((ChannelSpecNode)currElem).getFirstParametersChild();
        if(currElem == null)
            return result.toArray();

        for(INEDElement child : currElem) {
            // display only the following classes
            if ((child instanceof NedFileNode) ||
                    (child instanceof ImportNode) ||
                    (child instanceof ChannelNode) ||
                    (child instanceof ChannelInterfaceNode) ||
                    (child instanceof SimpleModuleNode) ||
                    (child instanceof CompoundModuleNode) ||
                    (child instanceof ModuleInterfaceNode) ||
                    (child instanceof GatesNode) ||
                    (child instanceof ParametersNode) ||
                    (child instanceof ConnectionsNode) ||
                    (child instanceof SubmodulesNode) ||
                    (child instanceof SubmoduleNode) ||
                    (child instanceof GateNode) ||
                    (child instanceof ParamNode) ||
                    (child instanceof PropertyNode) ||
                    (child instanceof ConnectionNode) ||
                    (child instanceof ConnectionGroupNode) ||
                    (child instanceof TypesNode) ||
                    (child instanceof ChannelSpecNode))
                result.add(child);
        }
        return result.toArray();
	}

	public boolean hasChildren(Object parent) {
        return getChildren(parent).length > 0;
	}
}