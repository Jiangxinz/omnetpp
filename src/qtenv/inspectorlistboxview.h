//==========================================================================
//  INSPECTORLISTBOXVIEW.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef INSPECTORLISTBOXVIEW_H
#define INSPECTORLISTBOXVIEW_H

#include <QTableView>

namespace omnetpp {

class cObject;

namespace qtenv {

class InspectorListBox;

class InspectorListBoxView : public QTableView
{
    Q_OBJECT
private:
    bool isSetColumnWidth;
    cObject *object;
    int type;

protected:
    void contextMenuEvent(QContextMenuEvent *event);

private slots:
    void contextMenuItemTriggerd();

public:
    InspectorListBoxView(QWidget *parent = nullptr);
    ~InspectorListBoxView();

    void setModel(InspectorListBox *model);
};

} // namespace qtenv
} // namespace omnetpp

#endif // INSPECTORLISTBOXVIEW_H
