package org.mayocat.shop.catalog.store.jdbi;

import java.util.List;

import javax.validation.Valid;

import org.mayocat.shop.catalog.model.Collection;
import org.mayocat.shop.catalog.model.Product;
import org.mayocat.shop.catalog.store.CollectionStore;
import org.mayocat.store.rdbms.dbi.dao.CollectionDAO;
import org.mayocat.model.EntityAndCount;
import org.mayocat.store.*;
import org.mayocat.store.rdbms.dbi.DBIEntityStore;
import org.mayocat.store.rdbms.dbi.MoveEntityInListOperation;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

@Component(hints={"jdbi", "default"})
public class DBICollectionStore extends DBIEntityStore implements CollectionStore, Initializable
{
    private static final String COLLECTION_TABLE_NAME = "collection";

    public static final String COLLECTION_POSITION = "collection.position";

    private CollectionDAO dao;

    public void create(Collection collection) throws EntityAlreadyExistsException, InvalidEntityException
    {
        if (this.dao.findBySlug(collection.getSlug(), getTenant()) != null) {
            throw new EntityAlreadyExistsException();
        }

        this.dao.begin();

        Long entityId = this.dao.createEntity(collection, COLLECTION_TABLE_NAME, getTenant());
        Integer lastIndex = this.dao.lastPosition(getTenant());
        this.dao.create(entityId, lastIndex == null ? 0 : ++lastIndex, collection);
        this.dao.insertTranslations(entityId, collection.getTranslations());

        this.dao.commit();
    }

    public void update(Collection collection) throws EntityDoesNotExistException, InvalidEntityException
    {
        this.dao.begin();

        Collection originalCollection = this.dao.findBySlug(collection.getSlug(), getTenant());
        if (originalCollection == null) {
            this.dao.commit();
            throw new EntityDoesNotExistException();
        }
        collection.setId(originalCollection.getId());
        Integer updatedRows = this.dao.update(collection);

        this.dao.commit();

        if (updatedRows <= 0) {
            throw new StoreException("No rows was updated when updating collection");
        }
    }

    @Override
    public void delete(@Valid Collection entity) throws EntityDoesNotExistException
    {
        Integer updatedRows = 0;
        this.dao.begin();
        updatedRows += this.dao.deleteEntityEntityById(COLLECTION_TABLE_NAME, entity.getId());
        updatedRows += this.dao.deleteEntityById(entity.getId());
        this.dao.commit();

        if (updatedRows <= 0) {
            throw new EntityDoesNotExistException("No rows was updated when trying to delete collection");
        }
    }


    public void moveCollection(String collectionToMove, String collectionToMoveRelativeTo,
            RelativePosition relativePosition) throws InvalidMoveOperation
    {
        this.dao.begin();

        List<Collection> allCollections = this.findAll();
        MoveEntityInListOperation<Collection> moveOp =
                new MoveEntityInListOperation<Collection>(allCollections, collectionToMove,
                        collectionToMoveRelativeTo, relativePosition);

        if (moveOp.hasMoved()) {
            this.dao.updatePositions(COLLECTION_TABLE_NAME, moveOp.getEntities(), moveOp.getPositions());
        }

        this.dao.commit();
    }

    @Override
    public List<EntityAndCount<Collection>> findAllWithProductCount()
    {
        return this.dao.findAllWithProductCount(getTenant());
    }

    public void addProduct(Collection collection, Product product)
    {
        this.dao.begin();
        Integer position = this.dao.lastProductPosition(collection);
        if (position == null) {
            position = 0;
        } else {
            position += 1;
        }
        this.dao.addProduct(collection, product, position);
        this.dao.commit();
    }

    public void removeProduct(Collection collection, Product product)
    {
        this.dao.removeProduct(collection, product);
    }

    public List<Collection> findAllForProduct(Product product)
    {
        return this.dao.findAllForProduct(product);
    }

    public List<Collection> findAll()
    {
        return this.dao.findAll(COLLECTION_TABLE_NAME, COLLECTION_POSITION, getTenant());
    }

    public List<Collection> findAll(Integer number, Integer offset)
    {
        return this.dao.findAll(COLLECTION_TABLE_NAME, COLLECTION_POSITION, getTenant(), number, offset);
    }

    @Override
    public Integer countAll()
    {
        return this.dao.countAll(COLLECTION_TABLE_NAME, getTenant());
    }

    public Collection findById(Long id)
    {
        return this.dao.findById(COLLECTION_TABLE_NAME, id);
    }

    public Collection findBySlug(String slug)
    {
        return this.dao.findBySlugWithTranslations(COLLECTION_TABLE_NAME, slug, getTenant());
    }

    public void initialize() throws InitializationException
    {
        this.dao = this.getDbi().onDemand(CollectionDAO.class);
        super.initialize();
    }
}