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
    DateField,
    Edit,
    TabbedForm,
    FormTab,
    DisabledInput,
    Responsive, 
    SimpleList,
    maxLength
} from 'react-admin';
import { unparse as convertToCSV } from 'papaparse/papaparse.min';
import moment from 'moment';
import Icon from '@material-ui/icons/Router';
import { withStyles } from '@material-ui/core/styles';
import { green, red } from '@material-ui/core/colors';

import { AppDateTimeFormat, DateTimeMomentFormat } from '../App';
import StatusField from './StatusField';
import CubeMap from './CubeMap';
//import PasswordInput from './PasswordInput';
import EissCubesShowActions from './ShowActions';

export const EissCubesIcon = Icon;

const styles = theme => ({
    title: {
        color: theme.palette.common.white
    },
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    },
    inlineField: { 
        display: 'inline-block',
        marginRight: theme.spacing.unit * 2, 
        minWidth: theme.spacing.unit * 24   
    },
    inline: { 
        display: 'inline-block', 
        marginRight: theme.spacing.unit * 2
    },
    longText: {
        minWidth: theme.spacing.unit * 66 
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
        fields: ['deviceID', 'name', 'online', 'timeStarted', 'lastPing']
    });

    downloadCSV(csv, 'EISS™Cubes');
};

const EissCubesTitle = withStyles(styles)(
    ({classes, title, record}) => (
        <div className={classes.title}>
            {title} {record && record.name && `${record.name}`}
        </div>
    )
);

const EissCubesListFilter = props => (
    <Filter {...props}>
        <SearchInput source='q' alwaysOn />
        <SelectInput source='online' label='Status' choices={[
            { id: true, name: 'ONLINE' },
            { id: false, name: 'OFFLINE' }
        ]} />
    </Filter>
);

export const EissCubesList = withStyles(styles)(
    ({ classes, ...props }) => (
        <List  
            title={<EissCubesTitle title='EISS™Cubes' />}
            filters={<EissCubesListFilter />}
            sort={{ field: 'name', order: 'ASC' }}
            perPage={10}
            exporter={exporter}
            {...props}
        >
            <Responsive
                small={
                    <SimpleList
                        linkType="show"
                        primaryText={record => record.name}
                        secondaryText={record => record.online === true ? <span style={{ color: green[500] }}>ONLINE</span> : <span style={{ color: red[500] }}>OFFLINE</span>}
                    />
                }
                medium={
                    <Datagrid classes={{ rowEven: classes.rowEven }} >
                        <TextField source='name' label='Name' />
                        <StatusField source='online' label='Status' />
                        <DateField source='timeStarted' label='Started' showTime options={AppDateTimeFormat} />
                        <DateField source='lastPing' label='Last ping' showTime options={AppDateTimeFormat} />
                        <ShowButton />
                    </Datagrid>
                }
            />
        </List>
    )
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
  
export const EissCubesEdit = withStyles(styles)(
    ({ classes, ...props }) => (
        <Edit  
            title={<EissCubesTitle title='Edit EISS™Cube -' />}
            {...props}
        >
            <TabbedForm>
                <FormTab label='identity'>
                    <DisabledInput label='ICCID' source='deviceID' className={classes.longText} validate={[ maxLength(20) ]} />
                    <TextInput label='Name' source='name' />
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
            </TabbedForm>
        </Edit>
    )
);
