import { useMediaQuery, Theme } from '@mui/material';
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
	//DateTimeInput,
    ReferenceInput,
    AutocompleteInput,
    ShowButton,
    SearchInput,
    useRefresh,
    usePermissions
} from 'react-admin';
import DateTimeFilterInput from './fields/DateTimeFilterInput';
import find from 'lodash/find';
import jsonExport from 'jsonexport/dist';
import { AppDateTimeFormat, DateTimeMomentFormat } from '../../App';
import { isSuper } from '../common/Roles';
import CommandStatusField from './fields/CommandStatusField';
import moment from 'moment';
import useRecursiveTimeout from '../../useRecursiveTimeout';
import { cmds } from '.';

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
        }, (_err, csv) => {
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
                <AutocompleteInput label='for EISS™Cube' optionText='name' sx={{minWidth: '14em'}} />
            </ReferenceInput>
            {isSuper(permissions) ?
                <ReferenceInput
                    source='group_id'
                    reference='grps'
                    sort={{ field: 'displayName', order: 'ASC' }}
                    allowEmpty
                >
                    <AutocompleteInput label='Group' optionText='displayName' sx={{minWidth: '14em'}} />
                </ReferenceInput>
            : null }
            <DateTimeFilterInput label='Created Before' source='timestamp_lte' sx={{minWidth: '14em'}} />
            <DateTimeFilterInput label='Created Since' source='timestamp_gte' sx={{minWidth: '14em'}} />
        </Filter>
    );
};

export const CommandList = (props: any) => {
	const isSmall = useMediaQuery<Theme>(theme => theme.breakpoints.down('sm'));
    const { permissions } = usePermissions();
    const { bulkActionButtons } = props;
    const refresh = useRefresh();

    useRecursiveTimeout(() => refresh, 10000);

    return (
        <List title='Commands' {...props}
            filters={<CommandFilter />}
            sort={{ field: 'created', order: 'DESC' }}
            perPage={10}
            exporter={exportCommandList}
            {...(isSuper(permissions) ? {bulkActionButtons } : {bulkActionButtons: false})}
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
                <Datagrid
                    sx={{
                        '& .RaDatagrid-rowOdd': {
                            backgroundColor: theme => theme.palette.grey[50],
                        },
                    }}
				>
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

					<CommandStatusField />

					<ShowButton />
                </Datagrid>
            )}
        </List>
    );
};

export default CommandList;
