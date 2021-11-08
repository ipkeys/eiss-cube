import { makeStyles, Theme } from '@material-ui/core/styles';
import { useMediaQuery } from '@material-ui/core';
import {
    downloadCSV,
    List,
    Filter,
    SimpleList,
    Datagrid,
    DateField,
    TextField,
    SelectField,
    ReferenceField,
    ReferenceInput,
    AutocompleteInput,
    ShowButton,
    SearchInput,
    useRefresh,
    usePermissions
} from 'react-admin';
import find from 'lodash/find';
import jsonExport from 'jsonexport/dist';
import { AppDateTimeFormat, DateTimeMomentFormat } from '../../App';
import { isSuper } from '../common/Roles';
import CommandStatusField from './fields/CommandStatusField';
import { DateTimeFilterInput } from './fields/DateTimePickerInput';
import moment from 'moment';
import useRecursiveTimeout from '../../useRecursiveTimeout';
import { NavCommandTitle } from '../common';
import { cmds } from '.';

const useStyles = makeStyles((theme: Theme) => ({ 
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    }
}));

const exportCommandList = (data: any) => {
    const records = data.map((record: any) => {
        const cmd = find(cmds, { 'id': record.command });

        return ({
            command: cmd?.name,
            'for EISS™Cubes': record.cubeName,
            created: moment(record.created).format(DateTimeMomentFormat),
            status: record.status
        })
    });

    jsonExport(records, {
        headers: ['command', 'for EISS™Cubes', 'created', 'status']
        }, (err, csv) => {
            downloadCSV(csv, 'EISS™Cubes Commands');
        }
    );
};

const CommandFilter = (props: any) => {
    const { permissions } = usePermissions();

    return (
        <Filter {...props}>
            <SearchInput source='q' alwaysOn />
            <ReferenceInput label='for EISS™Cube' source='cubeID' reference='cubes'>
                <AutocompleteInput optionText='name'/>
            </ReferenceInput>
            {isSuper(permissions) ? 
                <ReferenceInput 
                    source='group_id'
                    reference='grps'
                    sort={{ field: 'displayName', order: 'ASC' }}
                    allowEmpty
                >
                    <AutocompleteInput optionText='displayName' />
                </ReferenceInput>
            : null }
            <DateTimeFilterInput label='Created Before' source='timestamp_lte' 
                options={{ 
                    format: DateTimeMomentFormat, 
                    ampm: false, 
                    margin: 'dense', 
                    variant: 'inline', 
                    inputVariant: 'filled' 
                }} 
            />
            <DateTimeFilterInput label='Created Since' source='timestamp_gte' 
                options={{ 
                    format: DateTimeMomentFormat, 
                    ampm: false, 
                    margin: 'dense', 
                    variant: 'inline', 
                    inputVariant: 'filled' 
                }} 
            />
        </Filter>
    );
};

export const CommandList = (props: any) => {
    const classes = useStyles();
    const isSmall = useMediaQuery((theme: Theme) => theme.breakpoints.down('sm'));
    const { permissions } = usePermissions();    
    const { bulkActionButtons } = props;
    const refresh = useRefresh();
       
    useRecursiveTimeout(() => refresh, 10000);

    return (
        <List  
            title={<NavCommandTitle title='Commands' />}
            filters={<CommandFilter />}
            sort={{ field: 'created', order: 'DESC' }}
            perPage={10}
            exporter={exportCommandList}
            {...(isSuper(permissions) ? {bulkActionButtons } : {bulkActionButtons: false})}
            {...props}
        >
            {isSmall ? (
                <SimpleList
                    linkType='show'
                    primaryText={record => {
                        const cmd = record && record.command && find(cmds, { 'id': record.command });
                        return cmd.name;
                    }}
                    secondaryText={record => 
                        <ReferenceField source='cubeID' reference='cubes' link='show' {...record}>
                            <TextField source='name' />
                        </ReferenceField>
                    }
                />
            ) : (
                <Datagrid classes={{ rowEven: classes.rowEven }} >
                    <SelectField label='Command' source='command' choices={cmds} />
                    <ReferenceField label='for EISS™Cube' source='cubeID' reference='cubes' link='show'>
                        <TextField source='name' />
                    </ReferenceField>
                    {isSuper(permissions) &&
                    <ReferenceField source='group_id' label='Group' reference='grps' link={false} >
                        <TextField source='displayName' />
                    </ReferenceField>
                    }
                    <DateField label='Created' source='created' showTime options={AppDateTimeFormat} />
                    <CommandStatusField source='status' />
                    <ShowButton />
                </Datagrid>
            )}
        </List>
    );
};

export default CommandList;
