//==========================================================================
//   CPACKET.H  -  header for
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//
//  Declaration of the following classes:
//    cPacket : network packet class
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CPACKET_H
#define __CPACKET_H

#include "cmessage.h"

//==========================================================================

/**
 * DEPRECATED CLASS. See User Manual for replacement, and reasons of
 * deprecation.
 *
 * Network packet class. It adds protocol and PDU type to cMessage.
 * The message kind must be either MK_PACKET or MK_INFO for cPackets.
 *
 * @ingroup SimSupport
 */
class SIM_API cPacket : public cMessage
{
  protected:
    short _protocol;
    short _pdu;

  public:
    /** @name Constructors, destructor, assignment */
    //@{

    /**
     * Constructor.
     */
    explicit cPacket(const char *name=NULL, short protocol=0, short pdu=0) :
       cMessage(name,MK_PACKET) {_protocol=protocol;_pdu=pdu;}

    /**
     * Copy constructor.
     */
    cPacket(const cPacket& m);

    /**
     * Assignment operator. The name member doesn't get copied;
     * see cOwnedObject's operator=() for more details.
     */
    cPacket& operator=(const cPacket& m);
    //@}

    /** @name Redefined cObject member functions. */
    //@{

    /**
     * Creates and returns an exact copy of this object.
     * See cObject for more details.
     */
    virtual cPacket *dup() const  {return new cPacket(*this);}

    /**
     * Produces a one-line description of object contents.
     * See cObject for more details.
     */
    virtual std::string info() const;

    /**
     * Serializes the object into an MPI send buffer.
     * Used by the simulation kernel for parallel execution.
     * See cObject for more details.
     */
    virtual void netPack(cCommBuffer *buffer);

    /**
     * Deserializes the object from an MPI receive buffer
     * Used by the simulation kernel for parallel execution.
     * See cObject for more details.
     */
    virtual void netUnpack(cCommBuffer *buffer);
    //@}

    /** @name New attributes. */
    //@{

    /**
     * Returns the protocol ID.
     */
    short protocol() const     {return _protocol;}

    /**
     * Returns the PDU type.
     */
    short pdu() const          {return _pdu;}

    /**
     * Sets the protocol ID.
     */
    void setProtocol(short p) {_protocol=p;}

    /**
     * Sets the PDU type.
     */
    void setPdu(short p)      {_pdu=p;}
    //@}
};

#endif


