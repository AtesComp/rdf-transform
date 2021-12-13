package com.google.refine.rdf;

import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Record;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RecordModel {
    @JsonIgnore
    static private Project theProject = null;

    @JsonIgnore
    private int iRowIndex = -1;

    @JsonIgnore
    private Record theRecord = null;

    @JsonIgnore
    private boolean bSubRecords = false;

    @JsonIgnore
    private int iSubRecordRowStart = -1;

    @JsonIgnore
    private int iSubRecordRowEnd = -1;

    static void setProject(Project theProject) {
        RecordModel.theProject = theProject;
    }

    public RecordModel() {}

    public boolean isSet() {
        return (iRowIndex >= 0 || theRecord != null);
    }

    @JsonIgnore
    protected void setRootRow(int iRowIndex) {
        this.iRowIndex = iRowIndex;
    }

    @JsonIgnore
    protected void setRootRecord(Record theRecord) {
        this.theRecord = theRecord;
    }

    @JsonIgnore
    protected void setRowRecord(ResourceNode nodeParent) {
        this.iRowIndex = nodeParent.theRec.getRowIndex();
        this.theRecord = nodeParent.theRec.getRecord();
    }

    @JsonIgnore
    private int getRowIndex() {
        return this.iRowIndex;
    }

    @JsonIgnore
    private Record getRecord() {
        return this.theRecord;
    }

    protected void clear() {
        this.iRowIndex = -1;
        this.theRecord = null;

        this.bSubRecords = false;
        this.iSubRecordRowStart = -1;
        this.iSubRecordRowEnd = -1;
    }

    protected boolean isRecordMode() {
        return (this.theRecord != null);
    }

    protected boolean isRowMode() {
        return (this.theRecord == null);
    }

    protected int row() {
        return this.iRowIndex;
    }

    protected int rowStart() {
        if ( this.isRecordMode() ) {
            return this.theRecord.fromRowIndex;
        }
        return this.iRowIndex;
    }

    protected int rowEnd() {
        if ( this.isRecordMode() ) {
            return this.theRecord.toRowIndex;
        }
        return this.iRowIndex;
    }

    public void setSubRecord(String strColumnName) {
        if ( this.isSet() ) {
            bSubRecords = false;
            int iColumn = RecordModel.theProject.columnModel.getColumnByName(strColumnName).getCellIndex();

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
                Cell cell = RecordModel.theProject.rows.get(iRow).getCell( iColumn );
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
