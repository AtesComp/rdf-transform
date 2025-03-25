/*
 *  Class RecordModel
 *
 *  A Record Model utility class to manage the OpenRefine Record / Row modes
 *  at a particular RDF Transform node.
 *
 *  Copyright 2025 Keven L. Ates
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrefine.rdf.model.utils;

import com.google.refine.model.Cell;
import com.google.refine.model.Record;
import org.openrefine.rdf.model.Node;
import org.openrefine.rdf.model.ResourceNode;
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
        this.bRecordMode = (theRecord != null);
    }

    @JsonIgnore
    public void setMode(ResourceNode nodeProperty) {
        this.setMode(nodeProperty, false);
    }

    @JsonIgnore
    public void setMode(ResourceNode nodeProperty, boolean bPerRow) {
        // Set Row Mode...
        this.iRowIndex = nodeProperty.theRec.iRowIndex;
        // Set Record Mode only when Row Mode is off...
        if ( ! this.isRowMode() ) {
            this.theRecord = nodeProperty.theRec.theRecord;
            this.bRecordMode = (theRecord != null);
            this.bRecordPerRow = bPerRow;
            // NOTE: When bRecordPerRow is true, further processing will be in Row Mode as
            //       rowNext() will set iRowIndex values ( != -1 ).
        }
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
        this.bRecordMode = false;
        this.bRecordPerRow = false;

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

    public void rowReset() {
        this.iRowIndex = -1;
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
