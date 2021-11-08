import { makeStyles, Theme } from '@material-ui/core/styles';
import { useMediaQuery } from '@material-ui/core';
import { 
    List,
    Filter,
    Datagrid,
    TextField,
    FunctionField,
    ReferenceField,
    ReferenceInput,
    SelectInput,
    AutocompleteInput,
    ShowButton,
    SimpleList,
    SearchInput,
    useRefresh,
    usePermissions
} from 'react-admin';
import { isSuper } from '../common/Roles';
import { reportTypes } from '.';
import useRecursiveTimeout from '../../useRecursiveTimeout';
import { NavReportTitle } from '../common';

const useStyles = makeStyles((theme: Theme) => ({ 
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    }
}));

const ReportListFilter = (props: any) => {
    const { permissions } = usePermissions();

    return (
        <Filter {...props}>
            <SearchInput source='q' alwaysOn />
            {isSuper(permissions) && 
                <ReferenceInput source="group_id" reference="grps" sort={{ field: 'displayName', order: 'ASC' }} allowEmpty >
                    <AutocompleteInput optionText='displayName' />
                </ReferenceInput>
            }
    		<SelectInput label='Report type' source='type' margin='dense' choices={reportTypes} />
        </Filter>
    );
};

const ReportList = (props: any) => {
    const classes = useStyles();
    const isSmall = useMediaQuery((theme: Theme) => theme.breakpoints.down('sm'));
    const { permissions } = usePermissions();
    const refresh = useRefresh();

    useRecursiveTimeout(() => refresh, 10000);
    
    return (
		<List {...props}
			title={<NavReportTitle title='Reports' {...props} />}
			filters={<ReportListFilter {...props} />}
			sort={{ field: 'cubeID', order: 'ASC' }}
			perPage={10}
			exporter={false}
			bulkActionButtons={false}
		>
            {isSmall ? (
                <SimpleList
                    linkType='show'
                    primaryText={record => 
                        <ReferenceField source='cubeID' reference='cubes' link='show' {...record}>
                            <TextField source='name' />
                        </ReferenceField>
                    }
                    // @ts-ignore
                    secondaryText={record => reportTypes.find(t => t.id === record.type).name}
                />
            ) : (
                <Datagrid classes={{ rowEven: classes.rowEven }} >
                    <ReferenceField label='EISS™Cube' source='cubeID' reference='cubes' link='show'>
                        <TextField source='name' />
                    </ReferenceField>

                    {isSuper(permissions) &&
                        <ReferenceField source='group_id' label='Group' reference='grps' link={false} >
                            <TextField source='displayName' />
                        </ReferenceField>
                    }

                    <FunctionField label='Report type' sortBy='type'
                        // @ts-ignore
                        render={(rd: any) => reportTypes.find(t => t.id === rd.type).name}
                    />
                    <ShowButton />
                </Datagrid>
            )}
		</List>
    );
};

export default ReportList;
