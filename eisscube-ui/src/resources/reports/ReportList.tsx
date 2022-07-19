import { useMediaQuery, Theme } from '@mui/material';
import {
	Datagrid,
	Filter,
	List,
	SearchInput,
	SelectInput,
	ShowButton,
	TextField,
	SimpleList,
	ReferenceField,
	FunctionField,
	ReferenceInput,
	AutocompleteInput,
	usePermissions,
	useRefresh
} from 'react-admin';
import { isSuper } from '../common/Roles';
import { reportTypes } from '.';
import useRecursiveTimeout from '../../useRecursiveTimeout';

const ReportListFilter = (props: any) => {
	const { permissions } = usePermissions();

	return (
		<Filter {...props}>
			<SearchInput source='q' alwaysOn />
			<ReferenceInput label='for EISS™Cube' source='cubeID' reference='cubes'>
				<AutocompleteInput label='for EISS™Cube' optionText='name' sx={{minWidth: '14em'}} />
			</ReferenceInput>
			<SelectInput label='Report type' source='type' margin='dense' choices={reportTypes} sx={{minWidth: '14em'}} />
			{isSuper(permissions) &&
				<ReferenceInput source="group_id" reference="grps" sort={{ field: 'displayName', order: 'ASC' }} allowEmpty >
					<AutocompleteInput optionText='displayName' sx={{minWidth: '14em'}} />
				</ReferenceInput>
			}
		</Filter>
	);
};

const ReportList = (props: any) => {
	const isSmall = useMediaQuery<Theme>(theme => theme.breakpoints.down('sm'));
	const { permissions } = usePermissions();
	const refresh = useRefresh();

	useRecursiveTimeout(() => refresh, 10000);

    return (
		<List title='Reports' {...props}
			filters={<ReportListFilter />}
			sort={{ field: 'cubeID', order: 'ASC' }}
			perPage={10}
			exporter={false}
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
				<Datagrid
					bulkActionButtons={false}
					sx={{
						'& .RaDatagrid-rowOdd': {
							backgroundColor: theme => theme.palette.grey[50]
						}
					}}
				>
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
