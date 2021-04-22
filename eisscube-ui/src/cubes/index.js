import React from 'react';
import { 
    downloadCSV,
    List,
    Filter,
    Datagrid,
    TextField,
    SearchInput,
    TextInput,
    ReferenceField,
    ReferenceInput,
    SelectInput,
    AutocompleteInput,
    ShowButton,
    Show,
    DateField,
    Edit,
    TabbedForm,
    FormTab,
    Responsive, 
    SimpleList,
    maxLength,
    useRefresh
} from 'react-admin';
import jsonExport from 'jsonexport/dist';
import moment from 'moment';
import Icon from '@material-ui/icons/Router';
import { withStyles } from '@material-ui/core/styles';
import { green, red } from '@material-ui/core/colors';

import { AppDateTimeFormat, DateTimeMomentFormat, isSuperAdmin } from '../App';
import StatusField from './StatusField';
import CubeMap from './CubeMap';
import EissCubesShowActions from './ShowActions';
import Settings from './Settings';
import useRecursiveTimeout from '../useRecursiveTimeout';

export const EissCubesIcon = Icon;

const styles = theme => ({
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    },
    inlineField: { 
        display: 'inline-block',
        marginRight: theme.spacing(2), 
        minWidth: theme.spacing(24)   
    },
    inline: { 
        display: 'inline-block', 
        marginRight: theme.spacing(2)
    },
    longText: {
        minWidth: theme.spacing(66) 
    }
});

const exportCubeList = data => {
    const records = data.map(record => ({
        deviceID: record.deviceID,
        name: record.name,
        online: record.online,
        lastPing: moment(record.lastPing).format(DateTimeMomentFormat),
        timeStarted: moment(record.timeStarted).format(DateTimeMomentFormat)
    }));

    jsonExport(records, {
        headers: ['deviceID', 'name', 'online', 'timeStarted', 'lastPing']
        }, (err, csv) => {
            downloadCSV(csv, 'EISS™Cubes');
        }
    );
};

const EissCubesTitle = ({title, record}) => (
    <>
    {title} {record && record.name && `${record.name}`}
    </>
);

const EissCubesListFilter = props => (
    <Filter {...props}>
        <SearchInput source='q' alwaysOn />
        <SelectInput source='online' label='Status' margin='dense' choices={[
            { id: true, name: 'ONLINE' },
            { id: false, name: 'OFFLINE' }
        ]} />
        {isSuperAdmin(props.permissions) ? 
            <ReferenceInput source="group_id" reference="grps" sort={{ field: 'displayName', order: 'ASC' }} allowEmpty >
                <AutocompleteInput optionText='displayName' />
            </ReferenceInput>
        : null }
    </Filter>
);

export const EissCubesList = withStyles(styles)(
    ({ classes, permissions: p, bulkActionsButtons: btns, ...props }) => {
        const refresh = useRefresh();
        useRecursiveTimeout(() => refresh(), 10000);
        
        return (
            <List  
                title={<EissCubesTitle title='EISS™Cubes' />}
                filters={<EissCubesListFilter permissions={p} />}
                sort={{ field: 'name', order: 'ASC' }}
                perPage={10}
                exporter={exportCubeList}
                {...(isSuperAdmin(p) ? {bulkActionButtons: btns} : {bulkActionButtons: false})}
                {...props}
            >
                <Responsive
                    small={
                        <SimpleList
                            linkType='show'
                            primaryText={record => record.name}
                            secondaryText={record => record.online === true ? <span style={{ color: green[500] }}>ONLINE</span> : <span style={{ color: red[500] }}>OFFLINE</span>}
                        />
                    }
                    medium={
                        <Datagrid classes={{ rowEven: classes.rowEven }} >
                            <TextField source='name' label='Name' />
                            {isSuperAdmin(p) ?
                                <ReferenceField source='group_id' label='Group' reference='grps' link={false} allowEmpty={true} >
                                    <TextField source='displayName' />
                                </ReferenceField>
                            : 
                                null
                            }
                            <StatusField source='online' label='Status' />
                            <DateField source='timeStarted' label='Started' showTime options={AppDateTimeFormat} />
                            <DateField source='lastPing' label='Last ping' showTime options={AppDateTimeFormat} />
                            <ShowButton />
                        </Datagrid>
                    }
                />
            </List>
        );
    }
);

export const EissCubesShow = withStyles(styles)(
    ({ classes, ...props }) => (
        <Show
            title={<EissCubesTitle title='Manage EISS™Cube -' />}
            actions={<EissCubesShowActions />}
            {...props}
        >
            <CubeMap {...props}/>
        </Show>
    )
);
  
const validateICCID = maxLength(20);
const validateName = maxLength(50);

export const EissCubesEdit = withStyles(styles)(
    ({ classes, permissions: p, ...props }) => (
        <Edit
            title={<EissCubesTitle title='Edit EISS™Cube -' />}
            {...props}
        >
            <TabbedForm>
                <FormTab label='identity'>
                    {isSuperAdmin(p) ?
                        <ReferenceInput 
                            sort={{ field: 'displayName', order: 'ASC' }}
                            source="group_id" 
                            reference="grps"
                        >
                            <AutocompleteInput optionText='displayName' />
                        </ReferenceInput>
                    : 
                        null
                    }
                    <TextInput disabled label='ICCID' source='deviceID' className={classes.longText} validate={validateICCID} />
                    <TextInput label='Name' source='name' className={classes.longText} validate={validateName}/>
                </FormTab>
                <FormTab label='customer'>
                    <TextInput label='Customer ID' source='customerID' className={classes.longText} />
                    <TextInput label='Zone' source='zone' formClassName={classes.inline} />
                    <TextInput label='Subzone' source='subZone' formClassName={classes.inline} />
               </FormTab>
                <FormTab label='address'>
                    <TextInput label='Address' source='address' className={classes.longText} />
                    <TextInput label='City, State' source='city' formClassName={classes.inline} />
                    <TextInput label='Zip Code' source='zipCode' formClassName={classes.inline} />
                </FormTab>
                <FormTab label='settings'>
                    <Settings />
               </FormTab>
            </TabbedForm>
        </Edit>
    )
);
