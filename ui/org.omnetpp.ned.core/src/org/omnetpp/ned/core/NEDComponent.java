package org.omnetpp.ned.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.NEDElementConstants;
import org.omnetpp.ned.model.NEDSourceRegion;
import org.omnetpp.ned.model.ex.CompoundModuleNodeEx;
import org.omnetpp.ned.model.ex.ConnectionNodeEx;
import org.omnetpp.ned.model.ex.SubmoduleNodeEx;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.interfaces.IInterfaceTypeNode;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.interfaces.INEDTypeResolver;
import org.omnetpp.ned.model.interfaces.INedTypeNode;
import org.omnetpp.ned.model.notification.NEDModelEvent;
import org.omnetpp.ned.model.pojo.ExtendsNode;
import org.omnetpp.ned.model.pojo.GateNode;
import org.omnetpp.ned.model.pojo.NEDElementTags;
import org.omnetpp.ned.model.pojo.ParamNode;
import org.omnetpp.ned.model.pojo.PropertyNode;
import org.omnetpp.ned.model.pojo.SubmoduleNode;

/**
 * Default implementation of INEDComponent.
 *
 * @author rhornig, andras
 */
public class NEDComponent implements INEDTypeInfo, NEDElementTags, NEDElementConstants {

	protected INEDTypeResolver resolver;

	protected INedTypeNode componentNode;
	protected IFile file;
	
	private int debugId = lastDebugId++;
	private static int lastDebugId = 0;
	private static int debugRefreshInheritedCount = 0;
	private static int debugRefreshLocalCount = 0;

	// local stuff
    protected boolean needsLocalUpdate;
	protected Set<String> localInterfaces = new HashSet<String>();
	protected Map<String, PropertyNode> localProperties = new LinkedHashMap<String, PropertyNode>();
    protected Map<String, ParamNode> localParamDecls = new LinkedHashMap<String, ParamNode>();
    protected Map<String, ParamNode> localParamValues = new LinkedHashMap<String, ParamNode>();
	protected Map<String, GateNode> localGateDecls = new LinkedHashMap<String, GateNode>();
    protected Map<String, GateNode> localGateSizes = new LinkedHashMap<String, GateNode>();
	protected Map<String, INedTypeNode> localInnerTypes = new LinkedHashMap<String, INedTypeNode>();
	protected Map<String, SubmoduleNode> localSubmodules = new LinkedHashMap<String, SubmoduleNode>();
    protected HashSet<String> localUsedTypes = new HashSet<String>();

	// sum of all "local" stuff
	protected Map<String, INEDElement> localMembers = new LinkedHashMap<String, INEDElement>();

	// local plus inherited
	protected boolean needsUpdate;
	protected List<INEDTypeInfo> extendsChain = null;
	protected Set<String> allInterfaces = new HashSet<String>();
	protected Map<String, PropertyNode> allProperties = new LinkedHashMap<String, PropertyNode>();
    protected Map<String, ParamNode> allParams = new LinkedHashMap<String, ParamNode>();
    protected Map<String, ParamNode> allParamValues = new LinkedHashMap<String, ParamNode>();
    protected Map<String, GateNode> allGates = new LinkedHashMap<String, GateNode>();
    protected Map<String, GateNode> allGateSizes = new LinkedHashMap<String, GateNode>();
	protected Map<String, INedTypeNode> allInnerTypes = new LinkedHashMap<String, INedTypeNode>();
	protected Map<String, SubmoduleNode> allSubmodules = new LinkedHashMap<String, SubmoduleNode>();
    protected HashSet<String> allUsedTypes = new HashSet<String>();

	// sum of all local+inherited stuff
	protected Map<String, INEDElement> allMembers = new LinkedHashMap<String, INEDElement>();

//    // all types which extends this component
//    protected List<INEDTypeInfo> allDerivedTypes = new ArrayList<INEDTypeInfo>();
//    
//    // all types that contain instances (submodule, connection) of this type
//    protected List<INEDTypeInfo> allUsingTypes = new ArrayList<INEDTypeInfo>();

	// for local use
    interface IPredicate {
		public boolean matches(IHasName node);
	}

	/**
	 * Constructor
	 * @param node INEDElement tree to be wrapped
	 * @param nedfile file containing the definition
	 * @param res will be used to resolve inheritance (collect gates, params etc from base classes)
	 */
	public NEDComponent(INedTypeNode node, IFile nedfile, INEDTypeResolver res) {
		resolver = res;
		file = nedfile;
		componentNode = node;

		// register the created component in the INEDElement, so we will have access to it
        // directly from the model. We also want to listen on it, and invalidate localMembers etc
		// if anything changes.
		INEDTypeInfo oldTypeInfo = node.getNEDTypeInfo();
		if (oldTypeInfo != null)
			node.removeNEDChangeListener(oldTypeInfo);
		node.addNEDChangeListener(this);
        node.setNEDTypeInfo(this);

        // the inherited and local members will be collected on demand
        needsLocalUpdate = true;
		needsUpdate = true;
	}

    public INEDTypeResolver getResolver() {
        return resolver;
    }

    /**
	 * Collect elements (gates, params, etc) that match the predicate from the given section 
	 * (NED_PARAMETERS, NED_GATES, etc) into the map.
	 */
	@SuppressWarnings("unchecked")
	protected void collect(Map<String,? extends INEDElement> map, int sectionTagCode, IPredicate predicate) { 
		INEDElement section = componentNode.getFirstChildWithTag(sectionTagCode);
		if (section != null)
			for (INEDElement node : section) 
				if (node instanceof IHasName && predicate.matches((IHasName)node))
					((Map)map).put(((IHasName)node).getName(), node);
	}
	
	/**
	 * Collect the names from "extends" or "like" clauses into the given set
	 */
	protected void collectInheritance(Set<String> set, int tagCode) {
		Assert.isTrue(tagCode==NED_INTERFACE_NAME || tagCode==NED_EXTENDS);
		for (INEDElement child : getNEDElement())
			if (child.getTagCode()==tagCode)
				set.add(child.getAttribute(ExtendsNode.ATT_NAME));
	}

	/**
     * Collects all type names that are used in this module (submodule and connection types)
     * @param result storage for the used types
     */
    protected void collectTypesInCompoundModule(Set<String> result) {
        // this is only meaningful for CompoundModules so skip the others
        if (componentNode instanceof CompoundModuleNodeEx) {
        	// look for submodule types
        	INEDElement submodules = componentNode.getFirstChildWithTag(NED_SUBMODULES);
        	if (submodules != null)
        		for (INEDElement node : submodules)
        			if (node instanceof SubmoduleNodeEx)
        				result.add(((SubmoduleNodeEx)node).getEffectiveType());

        	// look for connection types
        	INEDElement connections = componentNode.getFirstChildWithTag(NED_CONNECTIONS);
        	if (connections != null)
        		for (INEDElement node : connections)
        			if (node instanceof ConnectionNodeEx)
        				result.add(((ConnectionNodeEx)node).getEffectiveType());
        }
    }

	/**
	 * Produce a list that starts with this type, and ends with the root.
	 */
	protected List<INEDTypeInfo> computeExtendsChain() {
	    List<INEDTypeInfo> result = new ArrayList<INEDTypeInfo>();
	    INEDTypeInfo currentComponent = this;
	    while (currentComponent != null) {
	    	result.add(currentComponent);
	    	currentComponent = currentComponent.getNEDElement().getFirstExtendsNEDTypeInfo();
	    }
	    return result;
	}

    /**
     * Refresh tables of local members
     */
    protected void refreshLocalMembersIfNeeded() {
    	if (!needsLocalUpdate)
    		return;
    	
        ++debugRefreshLocalCount;
        // System.out.println("NEDComponent for "+getName()+" localRefresh: " + refreshLocalCount);

        localInterfaces.clear();
        localProperties.clear();
        localParamDecls.clear();
        localParamValues.clear();
        localGateDecls.clear();
        localGateSizes.clear();
        localSubmodules.clear();
        localInnerTypes.clear();
        localMembers.clear();
        localUsedTypes.clear();

        // collect base types: interfaces extend other interfaces, modules implement interfaces
        collectInheritance(localInterfaces, getNEDElement() instanceof IInterfaceTypeNode ? NED_EXTENDS : NED_INTERFACE_NAME);
       
        // collect members from component declaration
        collect(localProperties, NED_PARAMETERS, new IPredicate() {
        	public boolean matches(IHasName node) {
        		return node.getTagCode()==NED_PROPERTY;
        	}});
        collect(localParamDecls, NED_PARAMETERS, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode()==NED_PARAM && ((ParamNode)node).getType() != NED_PARTYPE_NONE;
			}});
        collect(localParamValues, NED_PARAMETERS, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode()==NED_PARAM && StringUtils.isNotEmpty(((ParamNode)node).getValue());
			}});
        collect(localGateDecls, NED_GATES, new IPredicate() {
        	public boolean matches(IHasName node) {
        		return node.getTagCode()==NED_GATE && ((GateNode)node).getType() != NED_GATETYPE_NONE;
        	}});
        collect(localGateSizes, NED_GATES, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode()==NED_GATE && StringUtils.isNotEmpty(((GateNode)node).getVectorSize());
			}});
        collect(localInnerTypes, NED_TYPES, new IPredicate() {
			public boolean matches(IHasName node) {
				return node instanceof INedTypeNode;
			}});
        collect(localSubmodules, NED_SUBMODULES, new IPredicate() {
			public boolean matches(IHasName node) {
				return node.getTagCode()==NED_SUBMODULE;
			}});

        // collect them in one common hash table as well (we assume there's no name clash --
        // that should be checked beforehand by validation!)
        localMembers.putAll(localProperties);
        localMembers.putAll(localParamDecls);
        localMembers.putAll(localGateDecls);
        localMembers.putAll(localSubmodules);
        localMembers.putAll(localInnerTypes);

        // collect the types that were used in this module (meaningful only for compound modules)
        collectTypesInCompoundModule(localUsedTypes);

        needsLocalUpdate = false;
    }

	/**
	 * Collect all inherited parameters, gates, properties, submodules, etc.
	 */
	protected void refreshInheritedMembersIfNeeded() {
		if (!needsUpdate)
			return;
		
        ++debugRefreshInheritedCount;
        // System.out.println("NEDComponent for "+getName()+" inheritedRefresh: " + refreshInheritedCount);

        // first wee need our local members updated
        if (needsLocalUpdate)
            refreshLocalMembersIfNeeded();

        // determine extends chain
        extendsChain = computeExtendsChain();

        allInterfaces.clear();
		allProperties.clear();
		allParams.clear();
        allParamValues.clear();
		allGates.clear();
        allGateSizes.clear();
		allInnerTypes.clear();
		allSubmodules.clear();
		allMembers.clear();
		allUsedTypes.clear();

		// collect interfaces: what our base class implements (directly or indirectly), 
		// plus our interfaces and everything they extend (directly or indirectly)
		if (!(getNEDElement() instanceof IInterfaceTypeNode)) {
			INEDTypeInfo directBaseType = getNEDElement().getFirstExtendsNEDTypeInfo();
			if (directBaseType != null)
				allInterfaces.addAll(directBaseType.getInterfaces());
		}
		allInterfaces.addAll(localInterfaces);
		for (String interfaceName : localInterfaces) {
			INEDTypeInfo typeInfo = resolver.getComponent(interfaceName);
			if (typeInfo != null)
				allInterfaces.addAll(typeInfo.getInterfaces());
		}
		
        // collect all inherited members
		INEDTypeInfo[] forwardExtendsChain = extendsChain.toArray(new INEDTypeInfo[]{});
		ArrayUtils.reverse(forwardExtendsChain);
		for (INEDTypeInfo typeInfo : forwardExtendsChain) {
			Assert.isTrue(typeInfo instanceof NEDComponent);
			NEDComponent component = (NEDComponent)typeInfo;
			allProperties.putAll(component.getLocalProperties());
			allParams.putAll(component.getLocalParamDeclarations());
            allParamValues.putAll(component.getLocalParamAssignments());
			allGates.putAll(component.getLocalGateDeclarations());
            allGateSizes.putAll(component.getLocalGateSizes());
			allInnerTypes.putAll(component.getLocalInnerTypes());
			allSubmodules.putAll(component.getLocalSubmodules());
			allMembers.putAll(component.getLocalMembers());
			allUsedTypes.addAll(component.getLocalUsedTypes());
		}

// Not needed:
//        // additional tables for derived types and types using this one
//		allDerivedTypes.clear();
//
//		// collect all types that are derived from this
//        for (INEDTypeInfo currentComp : getResolver().getAllComponents()) {
//            if (currentComp == this)
//                continue;
//
//            // check for components the are extending us (directly or indirectly)
//            INEDElement element = currentComp.getNEDElement();
//            for (INEDElement child : element) {
//                if (child instanceof ExtendsNode) {
//                    String extendsName = ((ExtendsNode)child).getName();
//                    if (getName().equals(extendsName)) {
//                        allDerivedTypes.add(currentComp);
//                    }
//                }
//            }
//
//            // check for components that contain submodules, connections that use this type
//            if (currentComp.getLocalUsedTypes().contains(getName())) {
//                allUsingTypes.add(currentComp);
//            }
//        }
        needsUpdate = false;
	}

	/**
	 * Causes information about inherited members to be discarded, and
	 * later re-built on demand.
	 */
	public void invalidate() {
        needsLocalUpdate = true;
		needsUpdate = true;
	}

	public String getName() {
		return componentNode.getName();
	}

	public INedTypeNode getNEDElement() {
		return componentNode;
	}

	public IFile getNEDFile() {
		return file;
	}

	public INEDElement[] getNEDElementsAt(int line, int column) {
		ArrayList<INEDElement> list = new ArrayList<INEDElement>();
		NEDSourceRegion region = componentNode.getSourceRegion();
		if (region!=null && region.contains(line, column)) {
			list.add(componentNode);
			collectNEDElements(componentNode, line, column, list);
			return list.toArray(new INEDElement[list.size()]);
		}
		return null;
	}

	protected void collectNEDElements(INEDElement node, int line, int column, List<INEDElement> list) {
		for (INEDElement child : node) {
			NEDSourceRegion region = child.getSourceRegion();
			if (region!=null && region.contains(line, column)) {
				list.add(child);
				collectNEDElements(child, line, column, list); // children fall inside parent's region
			}
		}
	}

    public List<INEDTypeInfo> getExtendsChain() {
    	refreshInheritedMembersIfNeeded();
		return extendsChain;
	}

	public Set<String> getLocalInterfaces() {
		refreshLocalMembersIfNeeded();
        return localInterfaces;
	}

    public Map<String,ParamNode> getLocalParamDeclarations() {
    	refreshLocalMembersIfNeeded();
        return localParamDecls;
    }

    public Map<String,ParamNode> getLocalParamAssignments() {
    	refreshLocalMembersIfNeeded();
        return localParamValues;
    }

    public Map<String,PropertyNode> getLocalProperties() {
    	refreshLocalMembersIfNeeded();
        return localProperties;
    }

    public Map<String,GateNode> getLocalGateDeclarations() {
    	refreshLocalMembersIfNeeded();
        return localGateDecls;
    }

    public Map<String,GateNode> getLocalGateSizes() {
    	refreshLocalMembersIfNeeded();
        return localGateSizes;
    }

    public Map<String,INedTypeNode> getLocalInnerTypes() {
    	refreshLocalMembersIfNeeded();
        return localInnerTypes;
    }

    public Map<String,SubmoduleNode> getLocalSubmodules() {
    	refreshLocalMembersIfNeeded();
        return localSubmodules;
    }

    public Map<String,INEDElement> getLocalMembers() {
    	refreshLocalMembersIfNeeded();
        return localMembers;
    }

    public Set<String> getLocalUsedTypes() {
    	refreshLocalMembersIfNeeded();
        return localUsedTypes;
    }

    public Set<String> getInterfaces() {
    	refreshInheritedMembersIfNeeded();
        return allInterfaces;
    }

    public Map<String, ParamNode> getParamDeclarations() {
    	refreshInheritedMembersIfNeeded();
        return allParams;
    }

    public Map<String, ParamNode> getParamAssignments() {
    	refreshInheritedMembersIfNeeded();
        return allParamValues;
    }

    public Map<String, PropertyNode> getProperties() {
    	refreshInheritedMembersIfNeeded();
        return allProperties;
    }

    public Map<String, GateNode> getGateDeclarations() {
    	refreshInheritedMembersIfNeeded();
        return allGates;
    }

    public Map<String, GateNode> getGateSizes() {
    	refreshInheritedMembersIfNeeded();
        return allGateSizes;
    }

    public Map<String, INedTypeNode> getInnerTypes() {
    	refreshInheritedMembersIfNeeded();
        return allInnerTypes;
    }

    public Map<String, SubmoduleNode> getSubmodules() {
    	refreshInheritedMembersIfNeeded();
        return allSubmodules;
    }

    public Map<String, INEDElement> getMembers() {
    	refreshInheritedMembersIfNeeded();
        return allMembers;
    }

    public Set<String> getAllUsedTypes() {
    	refreshInheritedMembersIfNeeded();
        return allUsedTypes;
    }

//    public List<INEDTypeInfo> getAllDerivedTypes() {
//        if (needsUpdate)
//            refreshInheritedMembers();
//        return allDerivedTypes;
//    }
//
//    public List<INEDTypeInfo> getAllUsingTypes() {
//    	refreshInheritedMembersIfNeeded();
//        return allUsingTypes;
//    }

	public List<ParamNode> getParameterInheritanceChain(String parameterName) {
		List<ParamNode> result = new ArrayList<ParamNode>();
		for (INEDTypeInfo type : getExtendsChain())
			if (type.getLocalParamDeclarations().containsKey(parameterName))
				result.add((ParamNode) type.getLocalParamDeclarations().get(parameterName));
		return result;
	}

	public List<GateNode> getGateInheritanceChain(String gateName) {
		List<GateNode> result = new ArrayList<GateNode>();
		for (INEDTypeInfo type : getExtendsChain())
			if (type.getLocalGateDeclarations().containsKey(gateName))
				result.add(type.getLocalGateDeclarations().get(gateName));
		return result;
	}

	public List<PropertyNode> getPropertyInheritanceChain(String propertyName) {
		List<PropertyNode> result = new ArrayList<PropertyNode>();
		for (INEDTypeInfo type : getExtendsChain())
			if (type.getLocalProperties().containsKey(propertyName))
				result.add((PropertyNode) type.getLocalProperties().get(propertyName));
		return result;
	}

	/* (non-Javadoc)
     * @see java.lang.Object#toString()
     * Displays debugging info
     */
    @Override
    public String toString() {
        return "NEDComponent for "+getNEDElement();
    }

    public void modelChanged(NEDModelEvent event) {
        invalidate();
    }

    public void debugDump() {
    	System.out.println("NEDComponent: " + getNEDElement().toString() + " debugId=" + debugId);
    	if (needsUpdate || needsLocalUpdate)
    		System.out.println(" currently invalid (needs refresh)");
    	System.out.println("  extends chain: " + StringUtils.join(getExtendsChain(), ", "));
    	System.out.println("  local interfaces: " + StringUtils.join(localInterfaces, ", "));
    	System.out.println("  all interfaces: " + StringUtils.join(allInterfaces, ", "));
    	System.out.println("  local gates: " + StringUtils.join(localGateDecls.keySet(), ", "));
    	System.out.println("  all gates: " + StringUtils.join(allGates.keySet(), ", "));
    	System.out.println("  local parameters: " + StringUtils.join(localParamDecls.keySet(), ", "));
    	System.out.println("  all parameters: " + StringUtils.join(allParams.keySet(), ", "));
    	System.out.println("  local properties: " + StringUtils.join(localProperties.keySet(), ", "));
    	System.out.println("  all properties: " + StringUtils.join(allProperties.keySet(), ", "));
    	System.out.println("  local submodules: " + StringUtils.join(localSubmodules.keySet(), ", "));
    	System.out.println("  all submodules: " + StringUtils.join(allSubmodules.keySet(), ", "));
    }
}
