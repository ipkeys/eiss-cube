import React from 'react';
import { 
    downloadCSV,
    List,
    Filter,
    Datagrid,
    TextField,
    SearchInput,
    TextInput,
    SelectInput,
    ShowButton,
    Show,
    Create,
    DateField,
    Edit,
    EditButton,
    TabbedForm,
    FormTab,
    DisabledInput,
    
    required,
    minLength,
    maxLength
} from 'react-admin';
import { unparse as convertToCSV } from 'papaparse/papaparse.min';
import moment from 'moment';
import Icon from '@material-ui/icons/Router';
import { withStyles } from '@material-ui/core/styles';
import { common, grey } from '@material-ui/core/colors';

import { AppDateTimeFormat, DateTimeMomentFormat } from '../App';
import StatusField from './StatusField';
import CubeMap from './CubeMap';
import PasswordInput from './PasswordInput';
import EissCubesShowActions from './ShowActions';

export const EissCubesIcon = Icon;

const eissCubesStyles = theme => ({
    title: {
        color: common.white
    },
    rowEven: {
        backgroundColor: grey[100]
    },
    inline: { 
        display: 'inline-block', 
        marginRight: theme.spacing.unit 
    },
    longText: {
        minWidth: theme.spacing.unit * 64   
    }
});

const exporter = records => {
    const data = records.map(record => ({
        ...record,
        lastPing: moment(record.lastPing).format(DateTimeMomentFormat),
        timeStarted: moment(record.timeStarted).format(DateTimeMomentFormat)
    }));

    const csv = convertToCSV({
        data,
        fields: ['deviceID', 'online', 'timeStarted', 'lastPing']
    });

    downloadCSV(csv, 'EISS™Cubes');
};

const EissCubesTitle = withStyles(eissCubesStyles)(
    ({classes, title, record}) => (
        <div className={classes.title}>
            {title} {record && record.deviceID && `${record.deviceID}`}
        </div>
    )
);

const EissCubesListFilter = props => (
    <Filter {...props}>
        <SearchInput source="q" alwaysOn />
        <SelectInput source="online" label="Status" choices={[
            { id: true, name: 'ONLINE' },
            { id: false, name: 'OFFLINE' }
        ]} />
    </Filter>
);

export const EissCubesList = withStyles(eissCubesStyles)(
    ({ classes, ...props }) => (
        <List  
            title={<EissCubesTitle title="EISS™Cubes" />}
            filters={<EissCubesListFilter />}
            sort={{ field: 'deviceID', order: 'ASC' }}
            perPage={10}
            exporter={exporter}
            {...props}
        >
            <Datagrid classes={{ rowEven: classes.rowEven }} >
                <TextField source="deviceID" label="Name" />
                <StatusField source="online" label="Status" />
                <DateField source="timeStarted" label="Started" showTime options={AppDateTimeFormat} />
                <DateField source="lastPing" label="Last ping" showTime options={AppDateTimeFormat} />
                <ShowButton />
            </Datagrid>
        </List>
    )
);

export const EissCubesShow = withStyles(eissCubesStyles)(
    ({ classes, ...props }) => (
        <Show  
            title={<EissCubesTitle title="Manage EISS™Cube -" />}
            actions={<EissCubesShowActions />}
            {...props}
        >
            <CubeMap {...props}/>
        </Show>
    )
);
  
export const EissCubesEdit = withStyles(eissCubesStyles)(
    ({ classes, ...props }) => (
        <Edit  
            title={<EissCubesTitle title="Edit EISS™Cube -" />}
            {...props}
        >
            <TabbedForm>
                <FormTab label="identity">
                    <DisabledInput label="Device ID" source="deviceID" />
                    <DisabledInput label="Password" source="password" type="password" />
                    <TextInput label="SIM card" source="simCard" className={classes.longText} validate={[ maxLength(20) ]} />
                </FormTab>
                <FormTab label="customer">
                    <TextInput label="Customer ID" source="customerID" className={classes.longText} />
                    <TextInput label="Zone" source="zone" />
                    <TextInput label="Subzone" source="subZone" />
               </FormTab>
                <FormTab label="address">
                    <TextInput label="Address" source="address" className={classes.longText} />
                    <TextInput label="City, State" source="city" />
                    <TextInput label="Zip Code" source="zipCode" />
                </FormTab>
            </TabbedForm>
        </Edit>
    )
);

export const EissCubesCreate = withStyles(eissCubesStyles)(
    ({ classes, ...props }) => (
        <Create 
            title={<EissCubesTitle title="Create new EISS™Cube" />} 
            {...props}
        >
            <TabbedForm redirect="show">
                <FormTab label="identity">
                    <TextInput label="Device ID" source="deviceID" />
                    <PasswordInput label="Password" source="password" />
                    <TextInput label="SIM card" source="simCard" className={classes.longText} validate={[ maxLength(20) ]} />
                </FormTab>
                <FormTab label="customer">
                    <TextInput label="Customer ID" source="customerID" className={classes.longText} />
                    <TextInput label="Zone" source="zone" />
                    <TextInput label="Subzone" source="subZone" />
               </FormTab>
                <FormTab label="address">
                    <TextInput label="Address" source="address" className={classes.longText} />
                    <TextInput label="City, State" source="city" />
                    <TextInput label="Zip Code" source="zipCode" />
                </FormTab>
            </TabbedForm>
        </Create>
    )
);
