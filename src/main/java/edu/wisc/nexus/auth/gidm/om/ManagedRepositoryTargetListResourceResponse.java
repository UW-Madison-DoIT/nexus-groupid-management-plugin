package edu.wisc.nexus.auth.gidm.om;

import org.sonatype.nexus.rest.model.NexusResponse;
import org.sonatype.nexus.rest.model.RepositoryTargetListResource;

@com.thoughtworks.xstream.annotations.XStreamAlias(value = "repo-targets-list")
@javax.xml.bind.annotation.XmlRootElement(name = "repo-targets-list")
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
public class ManagedRepositoryTargetListResourceResponse extends NexusResponse implements java.io.Serializable {

    //--------------------------/
    //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    /**
     * Field data.
     */
    @javax.xml.bind.annotation.XmlElementWrapper(name = "data")
    @javax.xml.bind.annotation.XmlElement(name = "repo-targets-list-item")
    private java.util.List<RepositoryTargetListResource> data;

    //-----------/
    //- Methods -/
    //-----------/

    /**
     * Method addData.
     * 
     * @param repositoryTargetListResource
     */
    public void addData(RepositoryTargetListResource repositoryTargetListResource) {
        this.getData().add(repositoryTargetListResource);
    } //-- void addData( RepositoryTargetListResource )

    /**
     * Method getData.
     * 
     * @return List
     */
    public java.util.List<RepositoryTargetListResource> getData() {
        if (this.data == null) {
            this.data = new java.util.ArrayList<RepositoryTargetListResource>();
        }

        return this.data;
    } //-- java.util.List<RepositoryTargetListResource> getData()

    /**
     * Method removeData.
     * 
     * @param repositoryTargetListResource
     */
    public void removeData(RepositoryTargetListResource repositoryTargetListResource) {
        this.getData().remove(repositoryTargetListResource);
    } //-- void removeData( RepositoryTargetListResource )

    /**
     * Set the list of repository targets.
     * 
     * @param data
     */
    public void setData(java.util.List<RepositoryTargetListResource> data) {
        this.data = data;
    } //-- void setData( java.util.List )

}
