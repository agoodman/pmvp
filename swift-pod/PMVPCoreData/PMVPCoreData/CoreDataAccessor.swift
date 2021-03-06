//
//  CoreDataAccessor.swift
//  PMVP
//
//  Created by Aubrey Goodman on 5/1/19.
//  Copyright © 2019 Aubrey Goodman. All rights reserved.
//

import CoreData
import PMVP

public enum CoreDataResult<ResultType, E: Error> {
	case success(ResultType)
	case failure(E?)
}

open class CoreDataAccessor<K: Hashable & Comparable, L: NSManagedObject & LocalObject, P: Proxy<K>, E: Error> {

	private let contextFactory: () -> NSManagedObjectContext

	public let entityName: String

	private let keyName: String

	private let converter: Converter<K, L, P>

	public init(contextFactory: @escaping () -> NSManagedObjectContext, entityName: String, keyName: String, converter: Converter<K, L, P>) {
		self.contextFactory = contextFactory
		self.entityName = entityName
		self.keyName = keyName
		self.converter = converter
	}

	public final func objects(predicate: NSPredicate? = nil,
				 sortDescriptors: [NSSortDescriptor] = [],
				 limit: Int = 1000,
				 queue: DispatchQueue,
				 callback: @escaping (CoreDataResult<[P], E>) -> Void) {

		let entityName = self.entityName
		let context = self.contextFactory()
		let converter = self.converter
		context.perform {
			let fetchRequest: NSFetchRequest = NSFetchRequest<NSManagedObject>(entityName: entityName)
			fetchRequest.predicate = predicate
			fetchRequest.fetchLimit = limit
			fetchRequest.sortDescriptors = sortDescriptors
			if let rawResults: [NSManagedObject] = try? context.fetch(fetchRequest), let results = rawResults as? [L] {
				let proxies = results.map({ converter.toProxy($0) })
				queue.async { callback(.success(proxies)) }
			}
			else {
				queue.async { callback(.success([])) }
			}
		}
	}

	public final func upsert(_ objects: [K: P], queue: DispatchQueue, callback: @escaping (CoreDataResult<[P], E>) -> Void) {
		let keyName = self.keyName
		let entityName = self.entityName
		let context = self.contextFactory()
		context.perform { [weak self] in
			// first build a map of existing objects for the given keys
			let keys: [K] = [K](objects.keys)
			let fetchRequest: NSFetchRequest = NSFetchRequest<NSManagedObject>(entityName: entityName)
			fetchRequest.predicate = NSPredicate(format: "\(keyName) in %@", keys)

			var existing: [K: L] = [:]
			existing.reserveCapacity(objects.count)

			let results: [L]
			do {
				guard let rawResults: [L] = try context.fetch(fetchRequest) as? [L] else {
					queue.async { callback(CoreDataResult.failure(nil)) }
					return
				}
				results = rawResults
			}
			catch let error {
				queue.async { callback(.failure(error as? E)) }
				return
			}

			for obj: L in results {
				if let key: K = obj.value(forKey: keyName) as? K {
					existing[key] = obj
				}
			}

			// now iterate through objects and insert or update as needed
			for (key, obj) in objects {
				if let existing: L = existing[key] {
					self?.update(existing, with: obj)
				}
				else {
					self?.insert(obj, in: context)
				}
			}

			if context.hasChanges {
				_ = try? context.save()
			}

			let values: [P] = [P](objects.values)
			queue.async { callback(.success(values)) }
		}
	}

	public final func destroyObjects(for keys: [K], queue: DispatchQueue, callback: @escaping (CoreDataResult<[K], E>) -> Void) {
		let keyName = self.keyName
		let entityName = self.entityName
		let context = self.contextFactory()
		context.perform {
			let fetchRequest: NSFetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
			fetchRequest.predicate = NSPredicate(format: "\(keyName) in %@", keys)
			let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
			do {
				_ = try context.execute(deleteRequest)
				queue.async { callback(.success(keys)) }
			}
			catch let error {
				queue.async { callback(.failure(error as? E)) }
			}
		}
	}

	// MARK: - Required Methods

	open func update(_ object: L, with proxy: P) {
		fatalError("unimplemented \(#function)")
	}

	open func insert(_ object: P, in context: NSManagedObjectContext) {
		fatalError("unimplemented \(#function)")
	}

}
