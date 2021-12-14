package com.google.refine.rdf.utils;

import com.google.refine.rdf.Node;
import com.google.refine.rdf.ResourceNode;

import com.google.refine.model.Cell;
import com.google.refine.model.Record;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

@JsonIgnoreType
public class RecordModel {
    @JsonIgnore
    private final Node theNode;

    // For Row-based data processing...
    @JsonIgnore
    private int iRowIndex = -1;

    // For Record-based data processing...
    @JsonIgnore
    private Record theRecord = null;

    @JsonIgnore
    private boolean bRecordMode = false;

    @JsonIgnore
    private boolean bRecordPerRow = false;

    @JsonIgnore
    private boolean bSubRecords = false;

    @JsonIgnore
    private int iSubRecordRowStart = -1;

    @JsonIgnore
    private int iSubRecordRowEnd = -1;

    public RecordModel(Node theNode) {
        // Get the back reference to the node holding this object...
        this.theNode = theNode;
    }

    public boolean isSet() {
        return (iRowIndex >= 0 || theRecord != null);
    }

    @JsonIgnore
    public void setRootRow(int iRowIndex) {
        this.iRowIndex = iRowIndex;
    }

    @JsonIgnore
    public void setRootRecord(Record theRecord) {
        this.theRecord = theRecord;
        bRecordMode = (theRecord != null);
    }

    @JsonIgnore
    public void setLink(ResourceNode nodeLink) {
        this.setLink(nodeLink, false);
    }

    @JsonIgnore
    public void setLink(ResourceNode nodeLink, boolean bPerRow) {
        this.iRowIndex = nodeLink.theRec.iRowIndex;
        this.theRecord = nodeLink.theRec.theRecord;
        this.bRecordPerRow = bPerRow;
    }

    @JsonIgnore
    private int getRowIndex() {
        return this.iRowIndex;
    }

    @JsonIgnore
    private Record getRecord() {
        return this.theRecord;
    }

    public void clear() {
        this.iRowIndex = -1;
        this.theRecord = null;

        this.bSubRecords = false;
        this.iSubRecordRowStart = -1;
        this.iSubRecordRowEnd = -1;
    }

    public boolean isRecordMode() {
        return this.bRecordMode;
    }

    public boolean isRecordPerRow() {
        return this.bRecordPerRow;
    }

    public boolean isRowMode() {
        return (this.iRowIndex >= 0);
    }

    public int row() {
        return this.iRowIndex;
    }

    public boolean rowNext() {
        if ( this.isRecordMode() ) {
            // Set first row...
            if (this.iRowIndex < 0) {
                this.iRowIndex = this.theRecord.fromRowIndex;
                return true;
            }
            // Set next row...
            this.iRowIndex++;
            if (this.iRowIndex < this.theRecord.toRowIndex) {
                return true;
            }
            // No more rows...
            this.iRowIndex = -1;
        }
        return false;
    }

//    public int rowStart() {
//        if ( this.isRecordMode() ) {
//            return this.theRecord.fromRowIndex;
//        }
//        return this.iRowIndex;
//    }
//
//    public int rowEnd() {
//        if ( this.isRecordMode() ) {
//            return this.theRecord.toRowIndex;
//        }
//        return this.iRowIndex;
//    }

    public void setSubRecord(String strColumnName) {
        if ( this.isSet() ) {
            bSubRecords = false;
            int iColumn = this.theNode.getProject().columnModel.getColumnByName(strColumnName).getCellIndex();

            int iStart = this.theRecord.fromRowIndex;
            if (iSubRecordRowEnd >= 0) {
                iStart = iSubRecordRowEnd;
            }
            int iEnd = this.theRecord.toRowIndex;
            if (iStart == iEnd) {
                iSubRecordRowStart = -1;
                iSubRecordRowEnd = -1;
                bSubRecords = false;
                return;
            }

            iSubRecordRowStart = iStart;
            for (int iRow = iStart; iRow < iEnd; iRow++) {
                Cell cell = this.theNode.getProject().rows.get(iRow).getCell( iColumn );
                if ( ! ( cell == null || cell.value.toString().isEmpty() ) ) {
                    iSubRecordRowEnd = iRow + 1;
                    bSubRecords = true;
                    break;
                }
            }
            if (! bSubRecords) {
                iSubRecordRowEnd = iEnd;
                bSubRecords = true;
            }
        }
    }

    public boolean isSubRecordSet() {
        return (this.iSubRecordRowStart >= 0);
    }

}
